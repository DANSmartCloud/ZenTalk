package com.ZenTalk.server.user;

import com.ZenTalk.server.db.DatabaseService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class ProfileHandler {
    private final DatabaseService dbService;

    public ProfileHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    // 查看资料
    public Document viewProfile(String userId) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        Document user = users.find(new Document("userId", userId)).first();
        if (user == null) return null;
        // 只返回公开资料字段
        Document profile = new Document();
        profile.append("userId", user.getString("userId"));
        profile.append("nickname", user.getString("nickname"));
        profile.append("avatar", user.getString("avatar"));
        profile.append("gender", user.getString("gender"));
        profile.append("birthday", user.getString("birthday"));
        profile.append("tags", user.get("tags"));
        return profile;
    }

    // 编辑资料
    public boolean editProfile(String userId, String nickname, String avatar, String gender, String birthday) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        Document update = new Document();
        if (nickname != null) update.append("nickname", nickname);
        if (avatar != null) update.append("avatar", avatar);
        if (gender != null) update.append("gender", gender);
        if (birthday != null) update.append("birthday", birthday);
        if (update.isEmpty()) return false;
        users.updateOne(new Document("userId", userId), new Document("$set", update));
        return true;
    }
}