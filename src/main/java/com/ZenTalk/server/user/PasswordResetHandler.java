package com.ZenTalk.server.user;

import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.util.LogUtil;
import com.ZenTalk.server.util.MessageBundle;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import redis.clients.jedis.Jedis;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PasswordResetHandler {
    private final DatabaseService dbService;
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRE_MINUTES = 5;

    public PasswordResetHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    // 发送验证码
    public boolean sendVerificationCode(String account) {
        try {
            MongoDatabase db = dbService.getMongoDatabase();
            MongoCollection<Document> users = db.getCollection("users");
            Document query = new Document("$or", java.util.Arrays.asList(
                new Document("email", account),
                new Document("phone", account)
            ));
            Document user = users.find(query).first();
            if (user == null) return false;

            String code = generateRandomCode();
            // Use try-with-resources for Jedis
            try (Jedis redis = dbService.getJedisPool().getResource()) {
                redis.setex("verify:" + account, EXPIRE_MINUTES * 60, code);
            }

            // 实际发送逻辑应调用短信/邮件服务
            LogUtil.info(PasswordResetHandler.class, 
                MessageBundle.getMessage("verify.code.sent", account, code));
            return true;
        } catch (Exception e) {
            LogUtil.error(PasswordResetHandler.class, 
                MessageBundle.getMessage("verify.code.error", e.getMessage()));
            return false;
        }
    }

    // 验证验证码
    public boolean verifyCode(String account, String code) {
        try {
            // Use try-with-resources for Jedis
            try (Jedis redis = dbService.getJedisPool().getResource()) {
                String storedCode = redis.get("verify:" + account);
                return code != null && code.equals(storedCode);
            }
        } catch (Exception e) {
            LogUtil.error(PasswordResetHandler.class, 
                MessageBundle.getMessage("verify.code.error", e.getMessage()));
            return false;
        }
    }

    // 重置密码(需要验证码)
    public boolean resetPassword(String account, String code, String newPassword) {
        if (!verifyCode(account, code)) {
            return false;
        }
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        Document query = new Document("$or", java.util.Arrays.asList(
            new Document("email", account),
            new Document("phone", account)
        ));
        Document user = users.find(query).first();
        if (user == null) return false;
        String hashedPwd = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
        users.updateOne(query, new Document("$set", new Document("password", hashedPwd)));
        return true;
    }

    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}