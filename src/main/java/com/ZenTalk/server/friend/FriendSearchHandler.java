package com.ZenTalk.server.friend;

import com.ZenTalk.server.db.DatabaseService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class FriendSearchHandler {
    private final DatabaseService dbService;

    public FriendSearchHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    // 搜索用户（昵称/邮箱/手机号模糊）
    public List<Document> searchUser(String keyword) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        List<Document> result = new ArrayList<>();
        users.find(new Document("$or", java.util.Arrays.asList(
            new Document("nickname", new Document("$regex", keyword)),
            new Document("email", new Document("$regex", keyword)),
            new Document("phone", new Document("$regex", keyword))
        ))).into(result);
        return result;
    }
}