package com.ZenTalk.server.user;

import com.ZenTalk.server.db.DatabaseService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class SettingsHandler {
    private final DatabaseService dbService;

    public SettingsHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    // 修改头像
    public boolean updateAvatar(String userId, String avatarUrl) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        users.updateOne(new Document("userId", userId), new Document("$set", new Document("avatar", avatarUrl)));
        return true;
    }

    // 设置隐身
    public boolean setInvisible(String userId, boolean invisible) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        users.updateOne(new Document("userId", userId), new Document("$set", new Document("invisible", invisible)));
        return true;
    }

    // 更新标签
    public boolean updateTags(String userId, String[] tags) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        users.updateOne(new Document("userId", userId), new Document("$set", new Document("tags", java.util.Arrays.asList(tags))));
        return true;
    }

    // 修改密码
    public boolean changePassword(String userId, String oldPwd, String newPwd) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        Document user = users.find(new Document("userId", userId)).first();
        if (user == null) return false;
        if (!org.mindrot.jbcrypt.BCrypt.checkpw(oldPwd, user.getString("password"))) return false;
        String hashedPwd = org.mindrot.jbcrypt.BCrypt.hashpw(newPwd, org.mindrot.jbcrypt.BCrypt.gensalt());
        users.updateOne(new Document("userId", userId), new Document("$set", new Document("password", hashedPwd)));
        return true;
    }

    // 设置夜间模式
    public boolean setNightMode(String userId, boolean nightMode) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        users.updateOne(new Document("userId", userId), new Document("$set", new Document("nightMode", nightMode)));
        return true;
    }
}