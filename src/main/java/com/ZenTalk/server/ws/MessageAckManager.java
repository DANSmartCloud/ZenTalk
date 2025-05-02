package com.ZenTalk.server.ws;

import com.ZenTalk.model.Message;
import com.ZenTalk.repository.MessageRepository;
import com.ZenTalk.server.util.LogUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class MessageAckManager {
    private final MessageRepository messageRepository;
    // Removed WebSocketServer instance field
    // private final WebSocketServer webSocketServer;
    private final Map<String, PendingMessage> pendingMessages;
    private final DelayQueue<PendingMessage> retryQueue;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5秒后重试

    // Updated constructor to remove WebSocketServer parameter
    public MessageAckManager(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
        // this.webSocketServer = webSocketServer; // Removed assignment
        this.pendingMessages = new ConcurrentHashMap<>();
        this.retryQueue = new DelayQueue<>();
        startRetryWorker();
    }

    public void trackMessage(Message message) {
        PendingMessage pending = new PendingMessage(message);
        pendingMessages.put(message.getId(), pending);
        retryQueue.offer(pending);
    }

    public void handleAck(String userId, String messageId) {
        PendingMessage pending = pendingMessages.remove(messageId);
        if (pending != null) {
            messageRepository.updateMessageStatus(messageId, Message.MessageStatus.DELIVERED);
            LogUtil.debug(MessageAckManager.class,
                "Message " + messageId + " acknowledged by user " + userId);
        }
    }

    // Added methods to mark messages as sent or failed for external use (e.g., by WebSocketServer)
    public void markAsSent(String messageId) {
        PendingMessage pending = pendingMessages.remove(messageId);
        if (pending != null) {
            // Optionally update status if needed, or just remove tracking
            LogUtil.debug(MessageAckManager.class, "Message " + messageId + " marked as sent.");
        }
    }

    public void markAsFailed(String messageId) {
        PendingMessage pending = pendingMessages.remove(messageId);
        if (pending != null) {
            messageRepository.updateMessageStatus(messageId, Message.MessageStatus.FAILED);
            LogUtil.warn(MessageAckManager.class, "Message " + messageId + " marked as failed.");
            // Optionally store as offline if needed here, though MessageService might be better
            // messageRepository.storeOfflineMessage(pending.message);
        }
    }


    private void startRetryWorker() {
        Thread retryWorker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PendingMessage pending = retryQueue.take();
                    if (pendingMessages.containsKey(pending.message.getId())) {
                        if (pending.retryCount < MAX_RETRY_COUNT) {
                            retryMessage(pending);
                        } else {
                            handleMessageTimeout(pending.message);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        retryWorker.setDaemon(true);
        retryWorker.start();
    }

    private void retryMessage(PendingMessage pending) {
        try {
            // Call static WebSocketServer.sendMessage with recipient userId
            WebSocketServer.sendMessage(pending.message.getToUser(), pending.message);
            pending.retryCount++;
            pending.nextRetryTime = System.currentTimeMillis() + RETRY_DELAY_MS;
            retryQueue.offer(pending);

            LogUtil.info(MessageAckManager.class,
                "Retrying message " + pending.message.getId() +
                ", attempt " + pending.retryCount + " of " + MAX_RETRY_COUNT);
        } catch (Exception e) {
            LogUtil.error(MessageAckManager.class,
                "Failed to retry message " + pending.message.getId() + ": " + e.getMessage());
            // If retry fails, consider it timed out/failed immediately
            handleMessageTimeout(pending.message);
        }
    }

    private void handleMessageTimeout(Message message) {
        pendingMessages.remove(message.getId());
        messageRepository.updateMessageStatus(message.getId(), Message.MessageStatus.FAILED);
        // Storing offline message should likely be handled by MessageService when send fails initially
        // messageRepository.storeOfflineMessage(message);

        LogUtil.warn(MessageAckManager.class,
            "Message " + message.getId() + " timed out after " + MAX_RETRY_COUNT + " retries or failed to retry");
    }

    private static class PendingMessage implements Delayed {
        private final Message message;
        private int retryCount;
        private long nextRetryTime;

        PendingMessage(Message message) {
            this.message = message;
            this.retryCount = 0;
            this.nextRetryTime = System.currentTimeMillis() + RETRY_DELAY_MS;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(nextRetryTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS),
                              other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
