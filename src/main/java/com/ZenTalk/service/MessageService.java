package com.ZenTalk.service;

import com.ZenTalk.model.Message;
import com.ZenTalk.repository.MessageRepository;
import com.ZenTalk.server.ws.WebSocketServer;
import com.ZenTalk.server.util.LogUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MessageService {
    private final MessageRepository messageRepository;
    // Removed WebSocketServer instance field
    // private final WebSocketServer webSocketServer;
    private final Executor asyncExecutor;
    private static final int RETRY_MAX_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 1000;

    // Removed WebSocketServer from constructor
    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
        // this.webSocketServer = webSocketServer;
        this.asyncExecutor = Executors.newFixedThreadPool(10);
    }

    public Message sendTextMessage(String fromUser, String toUser, String content, String chatId) {
        Message message = new Message.Builder()
            .withFromUser(fromUser)
            .withToUser(toUser)
            .withContent(content)
            .withChatId(chatId)
            .withType(Message.MessageType.TEXT)
            .build();
        
        return sendMessage(message);
    }

    public Message sendEmojiMessage(String fromUser, String toUser, String emoji, String chatId) {
        Message message = new Message.Builder()
            .withFromUser(fromUser)
            .withToUser(toUser)
            .withContent(emoji)
            .withChatId(chatId)
            .withType(Message.MessageType.EMOJI)
            .build();
        
        return sendMessage(message);
    }

    public List<Message> sendImageMessages(String fromUser, String toUser, List<File> images, String chatId) {
        return images.stream()
            .map(image -> {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("fileName", image.getName());
                metadata.put("fileSize", String.valueOf(image.length()));
                
                Message message = new Message.Builder()
                    .withFromUser(fromUser)
                    .withToUser(toUser)
                    .withContent(image.getPath())
                    .withChatId(chatId)
                    .withType(Message.MessageType.IMAGE)
                    .withMetadata(metadata)
                    .build();
                
                return sendMessage(message);
            })
            .toList();
    }

    public Message sendFileMessage(String fromUser, String toUser, File file, String chatId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", file.getName());
        metadata.put("fileSize", String.valueOf(file.length()));
        
        Message message = new Message.Builder()
            .withFromUser(fromUser)
            .withToUser(toUser)
            .withContent(file.getPath())
            .withChatId(chatId)
            .withType(Message.MessageType.FILE)
            .withMetadata(metadata)
            .build();
        
        return sendMessage(message);
    }

    // Changed from private to public
    public Message sendMessage(Message message) {
        // 1. 保存消息到数据库
        Message savedMessage = messageRepository.save(message);
        
        // 2. 异步发送消息
        CompletableFuture.runAsync(() -> {
            int attempts = 0;
            while (attempts < RETRY_MAX_ATTEMPTS) {
                try {
                    // Use static method call
                    if (WebSocketServer.isUserOnline(message.getToUser())) {
                        WebSocketServer.sendMessage(message.getToUser(), message);
                        messageRepository.updateMessageStatus(message.getId(), Message.MessageStatus.DELIVERED);
                        return;
                    } else {
                        // 用户离线，存储离线消息
                        messageRepository.storeOfflineMessage(message);
                        messageRepository.updateMessageStatus(message.getId(), Message.MessageStatus.SENT);
                        return;
                    }
                } catch (Exception e) {
                    attempts++;
                    if (attempts == RETRY_MAX_ATTEMPTS) {
                        LogUtil.error(MessageService.class, 
                            "Failed to send message after " + RETRY_MAX_ATTEMPTS + " attempts: " + e.getMessage());
                        messageRepository.updateMessageStatus(message.getId(), Message.MessageStatus.FAILED);
                        return;
                    }
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, asyncExecutor);
        
        return savedMessage;
    }

    public List<Message> getMessageHistory(String chatId, int limit, int offset) {
        return messageRepository.findByChatId(chatId, limit, offset);
    }

    public void markMessagesAsRead(String chatId, String userId) {
        messageRepository.markAsRead(chatId, userId, LocalDateTime.now());
    }

    public List<Message> getOfflineMessages(String userId) {
        List<Message> messages = messageRepository.getOfflineMessages(userId);
        if (!messages.isEmpty()) {
            messageRepository.clearOfflineMessages(userId);
        }
        return messages;
    }

    public long getUnreadMessageCount(String userId) {
        return messageRepository.countUnreadMessages(userId);
    }

    public void deleteChat(String chatId) {
        messageRepository.deleteMessagesByChatId(chatId);
    }

    public void cleanupOldMessages(String chatId, int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        messageRepository.deleteMessagesBeforeTime(chatId, cutoffDate);
    }
}
