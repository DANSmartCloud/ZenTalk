package com.ZenTalk.server.user;

import com.ZenTalk.server.db.DatabaseService;
import redis.clients.jedis.Jedis; // Added import for Jedis
import com.ZenTalk.server.util.LogUtil; // Added import for LogUtil

public class UserStatusHandler {
    private final DatabaseService dbService;

    public UserStatusHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    // 设置用户在线状态
    public void setOnline(String userId, boolean online) {
        // Use try-with-resources for Jedis
        try (Jedis redis = dbService.getJedisPool().getResource()) {
            redis.set("user:status:" + userId, online ? "1" : "0");
        } catch (Exception e) {
            LogUtil.error(UserStatusHandler.class, "Error setting user status for " + userId + ": " + e.getMessage(), e);
        }
    }

    // 获取用户在线状态
    public boolean isOnline(String userId) {
        // Use try-with-resources for Jedis
        try (Jedis redis = dbService.getJedisPool().getResource()) {
            String status = redis.get("user:status:" + userId);
            return "1".equals(status);
        } catch (Exception e) {
            LogUtil.error(UserStatusHandler.class, "Error getting user status for " + userId + ": " + e.getMessage(), e);
            return false; // Assume offline on error
        }
    }
}