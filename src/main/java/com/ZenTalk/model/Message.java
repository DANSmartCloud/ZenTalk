package com.ZenTalk.model;

import java.time.LocalDateTime;
import java.util.Map;

public class Message {
    private String id;
    private String chatId;
    private String fromUser;
    private String toUser;
    private MessageType type;
    private String content;
    private Map<String, String> metadata;
    private MessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Message() {}

    public enum MessageType {
        TEXT,
        IMAGE,
        VIDEO,
        AUDIO,
        FILE,
        EMOJI,
        SYSTEM
    }

    public enum MessageStatus {
        SENDING,
        SENT,
        DELIVERED,
        READ,
        FAILED
    }

    public static class Builder {
        private final Message message;

        public Builder() {
            message = new Message();
            message.createdAt = LocalDateTime.now();
            message.updatedAt = LocalDateTime.now();
            message.status = MessageStatus.SENDING;
        }

        public Builder withId(String id) {
            message.id = id;
            return this;
        }

        public Builder withChatId(String chatId) {
            message.chatId = chatId;
            return this;
        }

        public Builder withFromUser(String fromUser) {
            message.fromUser = fromUser;
            return this;
        }

        public Builder withToUser(String toUser) {
            message.toUser = toUser;
            return this;
        }

        public Builder withType(MessageType type) {
            message.type = type;
            return this;
        }

        public Builder withContent(String content) {
            message.content = content;
            return this;
        }

        public Builder withMetadata(Map<String, String> metadata) {
            message.metadata = metadata;
            return this;
        }

        public Builder withStatus(MessageStatus status) {
            message.status = status;
            return this;
        }
        
        public Builder withCreatedAt(LocalDateTime createdAt) {
            message.createdAt = createdAt;
            return this;
        }
        
        public Builder withUpdatedAt(LocalDateTime updatedAt) {
            message.updatedAt = updatedAt;
            return this;
        }

        public Message build() {
            if (message.id == null) {
                message.id = java.util.UUID.randomUUID().toString();
            }
            return message;
        }
    }

    // Getters
    public String getId() { return id; }
    public String getChatId() { return chatId; }
    public String getFromUser() { return fromUser; }
    public String getToUser() { return toUser; }
    public MessageType getType() { return type; }
    public String getContent() { return content; }
    public Map<String, String> getMetadata() { return metadata; }
    public MessageStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Status update methods with timestamp update
    public void markAsSent() {
        this.status = MessageStatus.SENT;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsDelivered() {
        this.status = MessageStatus.DELIVERED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.status = MessageStatus.READ;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = MessageStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
}