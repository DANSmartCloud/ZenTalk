package com.ZenTalk.repository;

import com.ZenTalk.model.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class UserRepository implements Repository<User, String> {
    private final MongoDatabase database;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private static final String COLLECTION_NAME = "users";
    private static final String CACHE_PREFIX = "user:";
    private static final int CACHE_EXPIRE_MINUTES = 30;

    public UserRepository(MongoDatabase database, JedisPool jedisPool) {
        this.database = database;
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
        initializeIndexes();
    }

    private void initializeIndexes() {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        // 创建唯一索引
        collection.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("phone"), new IndexOptions().unique(true));
    }

    @Override
    public User save(User user) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        Document doc = toDocument(user);
        
        if (user.getId() == null) {
            collection.insertOne(doc);
        } else {
            collection.replaceOne(Filters.eq("_id", user.getId()), doc);
        }
        
        // 更新缓存
        try (Jedis jedis = jedisPool.getResource()) {
            String key = CACHE_PREFIX + user.getId();
            jedis.setex(key, TimeUnit.MINUTES.toSeconds(CACHE_EXPIRE_MINUTES), 
                objectMapper.writeValueAsString(user));
        } catch (Exception e) {
            // 缓存更新失败不影响主流程
        }
        
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        // 先查缓存
        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(CACHE_PREFIX + id);
            if (cached != null) {
                return Optional.of(objectMapper.readValue(cached, User.class));
            }
        } catch (Exception e) {
            // 缓存查询失败，继续查询数据库
        }

        // 查询数据库
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        Document doc = collection.find(Filters.eq("_id", id)).first();
        
        if (doc != null) {
            User user = fromDocument(doc);
            // 更新缓存
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(CACHE_PREFIX + id, 
                    TimeUnit.MINUTES.toSeconds(CACHE_EXPIRE_MINUTES),
                    objectMapper.writeValueAsString(user));
            } catch (Exception e) {
                // 缓存更新失败不影响主流程
            }
            return Optional.of(user);
        }
        
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.find().forEach(doc -> users.add(fromDocument(doc)));
        return users;
    }

    @Override
    public void deleteById(String id) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.deleteOne(Filters.eq("_id", id));
        // 删除缓存
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(CACHE_PREFIX + id);
        } catch (Exception e) {
            // 缓存删除失败不影响主流程
        }
    }

    @Override
    public boolean existsById(String id) {
        // 先查缓存
        try (Jedis jedis = jedisPool.getResource()) {
            if (jedis.exists(CACHE_PREFIX + id)) {
                return true;
            }
        } catch (Exception e) {
            // 缓存查询失败，继续查询数据库
        }

        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        return collection.countDocuments(Filters.eq("_id", id)) > 0;
    }

    public Optional<User> findByUsername(String username) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        Document doc = collection.find(Filters.eq("username", username)).first();
        return doc != null ? Optional.of(fromDocument(doc)) : Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        Document doc = collection.find(Filters.eq("email", email)).first();
        return doc != null ? Optional.of(fromDocument(doc)) : Optional.empty();
    }

    public Optional<User> findByPhone(String phone) {
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        Document doc = collection.find(Filters.eq("phone", phone)).first();
        return doc != null ? Optional.of(fromDocument(doc)) : Optional.empty();
    }

    private Document toDocument(User user) {
        return new Document()
            .append("_id", user.getId())
            .append("username", user.getUsername())
            // Do not store raw password
            // .append("password", user.getPassword()) 
            .append("email", user.getEmail())
            .append("phone", user.getPhone())
            .append("nickname", user.getNickname())
            .append("avatar", user.getAvatar())
            .append("gender", user.getGender())
            .append("birthday", user.getBirthday())
            .append("tags", user.getTags())
            .append("invisible", user.isInvisible())
            .append("status", user.getStatus())
            .append("createdAt", user.getCreatedAt())
            .append("updatedAt", user.getUpdatedAt());
    }

    private User fromDocument(Document doc) {
        User user = new User.Builder()
            .withUsername(doc.getString("username"))
            .withEmail(doc.getString("email"))
            .withPhone(doc.getString("phone"))
            .build();
            
        // 设置其他字段
        user.setNickname(doc.getString("nickname"));
        user.setAvatar(doc.getString("avatar"));
        user.setTags(doc.getList("tags", String.class));
        user.setInvisible(doc.getBoolean("invisible", false));
        user.setStatus(doc.getString("status"));
        
        return user;
    }
}
