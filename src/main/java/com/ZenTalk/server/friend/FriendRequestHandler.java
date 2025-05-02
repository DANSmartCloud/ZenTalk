package com.ZenTalk.server.friend;

import com.ZenTalk.server.db.DatabaseService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.UUID;

public class FriendRequestHandler {
    private final DatabaseService dbService;

    public FriendRequestHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    // 申请好友
    public boolean requestFriend(String fromUser, String toUser, String message) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> requests = db.getCollection("friend_requests");
        Document req = new Document("requestId", UUID.randomUUID().toString())
            .append("fromUser", fromUser)
            .append("toUser", toUser)
            .append("message", message)
            .append("status", "pending")
            .append("createdAt", System.currentTimeMillis());
        requests.insertOne(req);
        return true;
    }

    // 验证好友
    public boolean verifyFriend(String requestId, boolean accept) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> requests = db.getCollection("friend_requests");
        MongoCollection<Document> friends = db.getCollection("friends");
        Document req = requests.find(new Document("requestId", requestId)).first();
        if (req == null || !"pending".equals(req.getString("status"))) return false;
        requests.updateOne(new Document("requestId", requestId), new Document("$set", new Document("status", accept ? "accepted" : "rejected")));
        if (accept) {
            friends.insertOne(new Document("userId", req.getString("fromUser")).append("friendId", req.getString("toUser")));
            friends.insertOne(new Document("userId", req.getString("toUser")).append("friendId", req.getString("fromUser")));
        }
        return true;
    }

    // 删除好友
    public boolean deleteFriend(String user, String friendId) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> friends = db.getCollection("friends");
        friends.deleteOne(new Document("userId", user).append("friendId", friendId));
        return true;
    }

    // 修改备注
    public boolean updateRemark(String user, String friendId, String remark) {
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> friends = db.getCollection("friends");
        friends.updateOne(new Document("userId", user).append("friendId", friendId), new Document("$set", new Document("remark", remark)));
        return true;
    }
}