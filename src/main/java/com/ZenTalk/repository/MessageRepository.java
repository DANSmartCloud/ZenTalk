package com.ZenTalk.repository;

import com.ZenTalk.model.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends Repository<Message, String> {
    // 聊天相关查询
    List<Message> findByChatId(String chatId, int limit, int offset);
    List<Message> findByChatIdAndTimeBetween(String chatId, LocalDateTime start, LocalDateTime end);
    long countUnreadMessages(String userId);
    
    // 用户相关查询
    List<Message> findByToUserAndStatus(String userId, Message.MessageStatus status);
    List<Message> findByFromUser(String fromUser, int limit);
    
    // 消息状态更新
    void updateMessageStatus(String messageId, Message.MessageStatus newStatus);
    void markAsRead(String chatId, String userId, LocalDateTime beforeTime);
    
    // 批量操作
    void deleteMessagesByChatId(String chatId);
    void deleteMessagesBeforeTime(String chatId, LocalDateTime time);
    
    // 离线消息处理
    List<Message> getOfflineMessages(String userId);
    void storeOfflineMessage(Message message);
    void clearOfflineMessages(String userId);
}