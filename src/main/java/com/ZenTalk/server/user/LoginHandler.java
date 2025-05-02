package com.ZenTalk.server.user;

import com.ZenTalk.server.db.DatabaseService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

public class LoginHandler {
    private final DatabaseService dbService;

    public LoginHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    // 登录，支持用户名/邮箱/手机号
    public String login(String account, String password) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        Document query = new Document("$or", java.util.Arrays.asList(
            new Document("username", account),
            new Document("email", account),
            new Document("phone", account)
        ));
        Document user = users.find(query).first();
        if (user == null) return null;
        if (!BCrypt.checkpw(password, user.getString("password"))) return null;
        // 登录成功，生成token（简单UUID，生产建议用JWT）
        String token = java.util.UUID.randomUUID().toString();
        // 可存入Redis实现会话管理
        return token;
    }
}