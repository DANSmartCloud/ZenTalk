package com.ZenTalk.repository;

import com.ZenTalk.model.Message;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap; // Added import
import java.util.List;
import java.util.Map; // Added import
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors; // Added import for Collectors

public class MongoMessageRepository implements MessageRepository {
    private final MongoDatabase database;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private static final String COLLECTION_NAME = "messages";
    private static final String OFFLINE_MSG_PREFIX = "offline_msg:";
    private static final int MESSAGE_CACHE_MINUTES = 30;

    public MongoMessageRepository(MongoDatabase database, JedisPool jedisPool) {
        this.database = database;
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
        createIndexes();
    }

    private void createIndexes() {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.createIndex(Filters.eq("chatId", 1));
        collection.createIndex(Filters.eq("fromUser", 1));
        collection.createIndex(Filters.eq("toUser", 1));
        collection.createIndex(Filters.eq("status", 1));
        collection.createIndex(Filters.eq("createdAt", -1));
    }

    @Override
    public Message save(Message message) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        Document doc = toDocument(message);
        
        if (message.getId() == null) {
            collection.insertOne(doc);
        } else {
            collection.replaceOne(Filters.eq("_id", message.getId()), doc);
        }
        
        // 缓存最新消息
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "msg:" + message.getId();
            jedis.setex(key, 
                TimeUnit.MINUTES.toSeconds(MESSAGE_CACHE_MINUTES), 
                objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            // 缓存失败不影响主流程
        }
        
        return message;
    }

    @Override
    public Optional<Message> findById(String id) {
        // 先查缓存
        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get("msg:" + id);
            if (cached != null) {
                return Optional.of(objectMapper.readValue(cached, Message.class));
            }
        } catch (Exception e) {
            // 缓存查询失败，继续查询数据库
        }

        // 查询数据库
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        Document doc = collection.find(Filters.eq("_id", id)).first();
        
        if (doc != null) {
            return Optional.of(fromDocument(doc));
        }
        
        return Optional.empty();
    }

    @Override
    public boolean existsById(String id) {
        // Check cache first for efficiency, though existence check might not be cached
        // Directly check the database
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        return collection.countDocuments(Filters.eq("_id", id)) > 0;
    }

    @Override
    public List<Message> findByChatId(String chatId, int limit, int offset) {
        List<Message> messages = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.find(Filters.eq("chatId", chatId))
            .sort(new Document("createdAt", -1))
            .skip(offset)
            .limit(limit)
            .forEach(doc -> messages.add(fromDocument(doc)));
        return messages;
    }

    @Override
    public List<Message> findByChatIdAndTimeBetween(String chatId, LocalDateTime start, LocalDateTime end) {
        List<Message> messages = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.find(Filters.and(
            Filters.eq("chatId", chatId),
            Filters.gte("createdAt", start.toEpochSecond(ZoneOffset.UTC)),
            Filters.lte("createdAt", end.toEpochSecond(ZoneOffset.UTC))
        )).forEach(doc -> messages.add(fromDocument(doc)));
        return messages;
    }

    @Override
    public long countUnreadMessages(String userId) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        return collection.countDocuments(Filters.and(
            Filters.eq("toUser", userId),
            Filters.eq("status", Message.MessageStatus.DELIVERED.name())
        ));
    }

    @Override
    public List<Message> findByToUserAndStatus(String userId, Message.MessageStatus status) {
        List<Message> messages = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.find(Filters.and(
            Filters.eq("toUser", userId),
            Filters.eq("status", status.name())
        )).forEach(doc -> messages.add(fromDocument(doc)));
        return messages;
    }

    @Override
    public List<Message> findByFromUser(String fromUser, int limit) {
        List<Message> messages = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.find(Filters.eq("fromUser", fromUser))
            .sort(new Document("createdAt", -1))
            .limit(limit)
            .forEach(doc -> messages.add(fromDocument(doc)));
        return messages;
    }

    @Override
    public void updateMessageStatus(String messageId, Message.MessageStatus newStatus) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.updateOne(
            Filters.eq("_id", messageId),
            Updates.combine(
                Updates.set("status", newStatus.name()),
                Updates.set("updatedAt", LocalDateTime.now())
            )
        );
        
        // 更新缓存
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "msg:" + messageId;
            String cached = jedis.get(key);
            if (cached != null) {
                Message message = objectMapper.readValue(cached, Message.class);
                switch (newStatus) {
                    case SENT:
                        message.markAsSent();
                        break;
                    case DELIVERED:
                        message.markAsDelivered();
                        break;
                    case READ:
                        message.markAsRead();
                        break;
                    case FAILED:
                        message.markAsFailed();
                        break;
                }
                jedis.setex(key, 
                    TimeUnit.MINUTES.toSeconds(MESSAGE_CACHE_MINUTES),
                    objectMapper.writeValueAsString(message));
            }
        } catch (Exception e) {
            // 缓存更新失败不影响主流程
        }
    }

    @Override
    public void markAsRead(String chatId, String userId, LocalDateTime beforeTime) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.updateMany(
            Filters.and(
                Filters.eq("chatId", chatId),
                Filters.eq("toUser", userId),
                Filters.lt("createdAt", beforeTime.toEpochSecond(ZoneOffset.UTC)),
                Filters.ne("status", Message.MessageStatus.READ.name())
            ),
            Updates.combine(
                Updates.set("status", Message.MessageStatus.READ.name()),
                Updates.set("updatedAt", LocalDateTime.now())
            )
        );
    }

    @Override
    public void deleteMessagesByChatId(String chatId) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.deleteMany(Filters.eq("chatId", chatId));
    }

    @Override
    public void deleteMessagesBeforeTime(String chatId, LocalDateTime time) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.deleteMany(Filters.and(
            Filters.eq("chatId", chatId),
            Filters.lt("createdAt", time.toEpochSecond(ZoneOffset.UTC))
        ));
    }

    @Override
    public List<Message> getOfflineMessages(String userId) {
        List<Message> messages = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String key = OFFLINE_MSG_PREFIX + userId;
            List<String> msgList = jedis.lrange(key, 0, -1);
            for (String msgJson : msgList) {
                messages.add(objectMapper.readValue(msgJson, Message.class));
            }
        } catch (Exception e) {
            // Redis操作失败，记录日志
        }
        return messages;
    }

    @Override
    public void storeOfflineMessage(Message message) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = OFFLINE_MSG_PREFIX + message.getToUser();
            jedis.rpush(key, objectMapper.writeValueAsString(message));
            // 设置7天过期
            jedis.expire(key, TimeUnit.DAYS.toSeconds(7));
        } catch (Exception e) {
            // Redis操作失败，记录日志
        }
    }

    @Override
    public void clearOfflineMessages(String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = OFFLINE_MSG_PREFIX + userId;
            jedis.del(key);
        } catch (Exception e) {
            // Redis操作失败，记录日志
        }
    }

    @Override
    public List<Message> findAll() {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        // Implement findAll - fetch all documents and convert them
        // Be cautious with large collections, consider pagination or limiting
        List<Message> messages = new ArrayList<>();
        for (Document doc : collection.find()) {
            messages.add(fromDocument(doc));
        }
        return messages;
        // Alternative using streams (might be less readable for some):
        // return collection.find().map(this::fromDocument).into(new ArrayList<>());
    }

    @Override
    public void deleteById(String id) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.deleteOne(Filters.eq("_id", id));
        // Also remove from cache
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("msg:" + id);
        }
    }

    private Document toDocument(Message message) {
        return new Document()
            .append("_id", message.getId())
            .append("chatId", message.getChatId())
            .append("fromUser", message.getFromUser())
            .append("toUser", message.getToUser())
            .append("type", message.getType().name())
            .append("content", message.getContent())
            .append("metadata", message.getMetadata())
            .append("status", message.getStatus().name())
            .append("createdAt", message.getCreatedAt().toEpochSecond(ZoneOffset.UTC))
            .append("updatedAt", message.getUpdatedAt().toEpochSecond(ZoneOffset.UTC));
    }

    private Message fromDocument(Document doc) {
        Map<String, String> metadataMap = new HashMap<>();
        Document metadataDoc = doc.get("metadata", Document.class);
        if (metadataDoc != null) {
            for (Map.Entry<String, Object> entry : metadataDoc.entrySet()) {
                metadataMap.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
            }
        }

        return new Message.Builder()
            .withId(doc.getString("_id"))
            .withChatId(doc.getString("chatId"))
            .withFromUser(doc.getString("fromUser"))
            .withToUser(doc.getString("toUser"))
            .withType(Message.MessageType.valueOf(doc.getString("type")))
            .withContent(doc.getString("content"))
            .withMetadata(metadataMap) // Use the converted map
            .withStatus(Message.MessageStatus.valueOf(doc.getString("status")))
            .build();
    }
}