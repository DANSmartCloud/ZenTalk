package com.ZenTalk.server.db;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import com.ZenTalk.server.util.LogUtil;

public class MongoIndexConfig {
    public static void initializeIndexes(MongoDatabase database) {
        // Messages collection indexes
        createMessageIndexes(database);
        
        // Users collection indexes
        createUserIndexes(database);
        
        // Chats collection indexes
        createChatIndexes(database);
        
        LogUtil.info(MongoIndexConfig.class, "MongoDB索引初始化完成");
    }
    
    private static void createMessageIndexes(MongoDatabase database) {
        MongoCollection<Document> messages = database.getCollection("messages");
        
        // 注意：_id字段默认已经是唯一索引，不需要再创建
        
        // 聊天会话索引（按时间倒序）
        messages.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("chatId"),
                Indexes.descending("createdAt")
            )
        );
        
        // 发送者索引
        messages.createIndex(Indexes.ascending("fromUser"));
        
        // 接收者索引
        messages.createIndex(Indexes.ascending("toUser"));
        
        // 消息状态索引
        messages.createIndex(Indexes.ascending("status"));
        
        // 消息类型索引
        messages.createIndex(Indexes.ascending("type"));
        
        // 未读消息复合索引
        messages.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("toUser"),
                Indexes.ascending("status"),
                Indexes.descending("createdAt")
            )
        );
        
        // 时间范围查询索引
        messages.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("chatId"),
                Indexes.ascending("createdAt")
            )
        );
        
        // TTL索引（7天后自动删除离线消息）
        messages.createIndex(
            Indexes.ascending("createdAt"),
            new IndexOptions().expireAfter(7L * 24 * 60 * 60, java.util.concurrent.TimeUnit.SECONDS)
        );
    }
    
    private static void createUserIndexes(MongoDatabase database) {
        MongoCollection<Document> users = database.getCollection("users");
        
        // 注意：_id字段默认已经是唯一索引，不需要再创建
        
        // 用户名索引（唯一）
        users.createIndex(
            Indexes.ascending("username"),
            new IndexOptions().unique(true)
        );
        
        // 邮箱索引（唯一）
        users.createIndex(
            Indexes.ascending("email"),
            new IndexOptions().unique(true)
        );
        
        // 手机号索引（唯一）
        users.createIndex(
            Indexes.ascending("phone"),
            new IndexOptions().unique(true)
        );
        
        // 在线状态索引
        users.createIndex(Indexes.ascending("status"));
        
        // 最后活跃时间索引
        users.createIndex(Indexes.descending("lastActiveAt"));
    }
    
    private static void createChatIndexes(MongoDatabase database) {
        MongoCollection<Document> chats = database.getCollection("chats");
        
        // 注意：_id字段默认已经是唯一索引，不需要再创建
        
        // 参与者复合索引
        chats.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("userId"),
                Indexes.ascending("friendId")
            )
        );
        
        // 创建时间索引
        chats.createIndex(Indexes.descending("createdAt"));
        
        // 最后消息时间索引
        chats.createIndex(Indexes.descending("lastMessageAt"));
    }
}
