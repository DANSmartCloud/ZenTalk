package com.ZenTalk.server.chat;

import com.ZenTalk.model.Message;
import com.ZenTalk.repository.MessageRepository;
import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.util.StringResources;
import com.ZenTalk.server.util.LogUtil;
import com.ZenTalk.service.MessageService;
import com.ZenTalk.server.ws.MessageAckManager;
import javax.websocket.Session;
import redis.clients.jedis.Jedis;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHandler {
    private final DatabaseService dbService;
    private final MessageService messageService;
    private final MessageAckManager messageAckManager;
    private final MessageRepository messageRepository; // Added messageRepository
    private final Map<String, Session> onlineUsers = new ConcurrentHashMap<>(); // Added onlineUsers map

    // Updated constructor to accept MessageService and MessageAckManager
    public ChatHandler(DatabaseService dbService, MessageService messageService, MessageAckManager messageAckManager, MessageRepository messageRepository) {
        this.dbService = dbService;
        this.messageService = messageService;
        this.messageAckManager = messageAckManager;
        this.messageRepository = messageRepository;
    }

    public void sendMessage(String fromUser, String toUser, String message) {
        // 检查toUser是否在线（假设有user:status:{uid}，1为在线，0为离线）
        try (Jedis redis = dbService.getJedisPool().getResource()) {
            String statusKey = StringResources.getString("user.status.key") + toUser;
            String status = redis.get(statusKey);
            if (status == null || !status.equals("1")) {
                // 离线，消息存入Redis临时队列
                String offlineKey = StringResources.getString("offline.msg.key") + toUser;
                redis.rpush(offlineKey, fromUser + ":" + message);
            } else {
                // 在线，直接推送（此处仅打印，实际应推送到前端或WebSocket）
                LogUtil.info(ChatHandler.class, String.format("推送消息: %s <- %s: %s", toUser, fromUser, message));
            }
        }
    }

    // 用户上线时调用，推送离线消息
    public void deliverOfflineMessages(String user) {
        try (Jedis redis = dbService.getJedisPool().getResource()) {
            String offlineKey = StringResources.getString("offline.msg.key") + user;
            while (true) {
                String msg = redis.lpop(offlineKey);
                if (msg == null) break;
                LogUtil.info(ChatHandler.class, String.format("离线消息: %s <- %s", user, msg));
                // 实际应推送到前端
            }
        }
    }

    // 添加聊天会话
    public boolean addChat(String userId, String friendId) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> chats = db.getCollection("chats");
        Document chat = new Document("chatId", UUID.randomUUID().toString())
            .append("userId", userId)
            .append("friendId", friendId)
            .append("createdAt", System.currentTimeMillis());
        chats.insertOne(chat);
        return true;
    }

    // 删除聊天会话
    public boolean deleteChat(String userId, String chatId) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> chats = db.getCollection("chats");
        chats.deleteOne(new Document("userId", userId).append("chatId", chatId));
        return true;
    }

    // 拉取历史消息
    public List<Document> getHistory(String chatId, int limit) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> messages = db.getCollection("messages");
        return messages.find(new Document("chatId", chatId))
            .sort(new Document("timestamp", -1)).limit(limit).into(new java.util.ArrayList<>());
    }

    public void handleMessage(Session session, Message message) {
        // Validation logic might be needed here or moved to MessageService
        // if (!isValidMessage(message)) {
        //     return;
        // }

        // Use MessageService to handle saving and broadcasting
        try {
            // Corrected method call from processAndSendMessage to sendMessage
            messageService.sendMessage(message);
        } catch (Exception e) {
            LogUtil.error(ChatHandler.class, "Error handling message: " + e.getMessage(), e);
            // Optionally send an error message back to the sender
        }

        // Acknowledgment logic might be handled by MessageAckManager or WebSocketServer
        // messageAckManager.handleMessageAcknowledgement(message);

        // Offline message handling is likely part of MessageService now
        // if (isPrivateChat(message.getChatId())) {
        //     String recipientId = getRecipientId(message.getChatId(), message.getFromUser());
        //     if (!isUserOnline(recipientId)) {
        //         messageRepository.storeOfflineMessage(message);
        //     }
        // }
    }

    // Added Session parameter to handleClose
    public void handleClose(Session session, int statusCode, String reason) {
        // String userId = (String) session.getUserProperties().get("userId");
        String userId = com.ZenTalk.server.ws.WebSocketServer.getSessionUser(session); // Use static method from WebSocketServer
        if (userId != null) {
            onlineUsers.remove(userId); // Remove from local map
            // Use Redis to update global online status (moved to UserStatusHandler or similar)
            // try (Jedis jedis = dbService.getJedisPool().getResource()) {
            //     jedis.srem("online_users", userId);
            // }
            LogUtil.info(ChatHandler.class, "User disconnected: " + userId + " Reason: " + reason + " (" + statusCode + ")");
        }
    }
    
    // 添加handleConnect方法
    public void handleConnect(Session session, String userId) {
        if (userId != null) {
            onlineUsers.put(userId, session);
            LogUtil.info(ChatHandler.class, "User connected: " + userId);
            // 可以在这里处理用户上线后的逻辑，如推送离线消息等
            deliverOfflineMessages(userId);
        }
    }
    
    // 添加处理离线消息的方法
    public void handleOfflineMessage(String userId, Message message) {
        // 实现离线消息处理逻辑
        try {
            messageRepository.storeOfflineMessage(message);
            LogUtil.info(ChatHandler.class, "Stored offline message for user: " + userId);
        } catch (Exception e) {
            LogUtil.error(ChatHandler.class, "Error storing offline message: " + e.getMessage(), e);
        }
    }

    // Helper methods (consider moving to appropriate services)
    private boolean isPrivateChat(String chatId) {
        // Implement logic to determine if chatId represents a private chat
        return !chatId.startsWith("group:"); // Example logic
    }

    private String getRecipientId(String chatId, String senderId) {
        // Implement logic to get the recipient ID for a private chat
        // This might involve querying chat metadata
        return "recipient_user_id"; // Placeholder
    }

    private boolean isUserOnline(String userId) {
        // Check local map first, then potentially Redis via UserStatusHandler
        return onlineUsers.containsKey(userId);
    }

    // 添加 handleError 方法处理 WebSocket 错误
    public void handleError(Session session, Throwable throwable) {
        String userId = com.ZenTalk.server.ws.WebSocketServer.getSessionUser(session);
        LogUtil.error(ChatHandler.class, "WebSocket error for user " + (userId != null ? userId : "<unknown>") + ": " + throwable.getMessage(), throwable);
        // 可以在这里添加额外的错误处理逻辑，如通知用户、记录错误等
    }
    
    // 添加 handleHistoryRequest 方法处理历史消息请求
    public void handleHistoryRequest(Session session, String userId) {
        try {
            // 获取用户的所有聊天会话
            MongoDatabase db = dbService.getMongoDatabase();
            MongoCollection<Document> chats = db.getCollection("chats");
            List<Document> userChats = chats.find(new Document("userId", userId))
                .into(new java.util.ArrayList<>());
            
            // 对每个聊天会话，获取最近的消息并发送给用户
            for (Document chat : userChats) {
                String chatId = chat.getString("chatId");
                List<Document> messages = getHistory(chatId, 20); // 获取最近20条消息
                
                // 将消息转换为Message对象并发送
                for (Document msgDoc : messages) {
                    Message.Builder builder = new Message.Builder()
                        .withId(msgDoc.getString("msgId"))
                        .withChatId(msgDoc.getString("chatId"))
                        .withFromUser(msgDoc.getString("fromUser"))
                        .withToUser(msgDoc.getString("toUser"))
                        .withContent(msgDoc.getString("content"));
                    
                    // 设置消息类型
                    String type = msgDoc.getString("type");
                    switch (type) {
                        case "text":
                            builder.withType(Message.MessageType.TEXT);
                            break;
                        case "image":
                            builder.withType(Message.MessageType.IMAGE);
                            break;
                        case "file":
                            builder.withType(Message.MessageType.FILE);
                            break;
                        case "emoji":
                            builder.withType(Message.MessageType.EMOJI);
                            break;
                        default:
                            builder.withType(Message.MessageType.TEXT);
                            break;
                    }
                    
                    // 设置时间戳
                    Long timestamp = msgDoc.getLong("timestamp");
                    if (timestamp != null) {
                        java.time.LocalDateTime createdAt = java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(timestamp),
                            java.time.ZoneId.systemDefault());
                        builder.withCreatedAt(createdAt);
                    }
                    
                    Message message = builder.build();
                    com.ZenTalk.server.ws.WebSocketServer.sendMessage(userId, message);
                }
            }
            
            LogUtil.info(ChatHandler.class, "Sent history messages to user: " + userId);
        } catch (Exception e) {
            LogUtil.error(ChatHandler.class, "Error sending history messages to user " + userId + ": " + e.getMessage(), e);
        }
    }

    // Removed isValidMessage as validation might be elsewhere
    // private boolean isValidMessage(Message message) { ... }
}