package com.ZenTalk.server.chat;

import com.ZenTalk.server.util.MessageBundle;

import com.ZenTalk.server.db.DatabaseService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class MessageHandler {
    private final DatabaseService dbService;

    public MessageHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    // 发送文本消息
    public boolean sendText(String fromUser, String toUser, String text) {
        return sendMessage(fromUser, toUser, text, "text");
    }

    // 发送Emoji
    public boolean sendEmoji(String fromUser, String toUser, String emoji) {
        return sendMessage(fromUser, toUser, emoji, "emoji");
    }

    // 发送图片（批量）
    public boolean sendImages(String fromUser, String toUser, List<File> images) {
        return sendTextWithImages(fromUser, toUser, null, images);
    }

    // 发送文件
    public boolean sendFile(String fromUser, String toUser, File file) {
        return sendMessage(fromUser, toUser, file.getName(), "file");
    }

    // 发送文本+图片消息（支持多图，图片在文本下方）
    public boolean sendTextWithImages(String fromUser, String toUser, String text, List<File> images) {
        boolean sent = false;
        if (text != null && !text.isEmpty()) {
            sendMessage(fromUser, toUser, text, "text");
            sent = true;
        }
        if (images != null && !images.isEmpty()) {
            for (File img : images) {
                sendMessage(fromUser, toUser, img.getName(), "image");
                sent = true;
            }
        }
        return sent;
    }

    // 拍摄图片+文字一起发送
    public boolean sendCapturedImageWithText(String fromUser, String toUser, File capturedImage, String text) {
        List<File> images = new ArrayList<>();
        images.add(capturedImage);
        return sendTextWithImages(fromUser, toUser, text, images);
    }

    // 删除聊天
    public boolean deleteChat(String user, String chatId) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> messages = db.getCollection("messages");
        messages.deleteMany(new Document("chatId", chatId).append("fromUser", user));
        return true;
    }

    // 新消息通知
    public void notifyNewMessage(String toUser, String message) {
        com.ZenTalk.server.util.AudioUtil.playNotificationSound();
        System.out.println(MessageBundle.getMessage("notification.new.message", toUser, message));
    }

    // 内部通用消息存储
    private boolean sendMessage(String fromUser, String toUser, String content, String type) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> messages = db.getCollection("messages");
        String chatId = getOrCreateChatId(fromUser, toUser, db);
        Document msg = new Document("msgId", UUID.randomUUID().toString())
            .append("chatId", chatId)
            .append("fromUser", fromUser)
            .append("toUser", toUser)
            .append("type", type)
            .append("content", content)
            .append("timestamp", System.currentTimeMillis());
        messages.insertOne(msg);
        notifyNewMessage(toUser, content);
        return true;
    }

    // 获取或创建会话ID
    private String getOrCreateChatId(String userA, String userB, MongoDatabase db) {
        MongoCollection<Document> chats = db.getCollection("chats");
        Document chat = chats.find(new Document("userId", userA).append("friendId", userB)).first();
        if (chat != null) return chat.getString("chatId");
        String chatId = UUID.randomUUID().toString();
        chats.insertOne(new Document("chatId", chatId).append("userId", userA).append("friendId", userB));
        return chatId;
    }
}