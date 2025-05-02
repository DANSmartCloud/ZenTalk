package com.ZenTalk.server.command;

import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.util.LogUtil;
import com.ZenTalk.server.util.MessageBundle;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class BanUserCommand {
    private final DatabaseService dbService;

    public BanUserCommand(DatabaseService dbService) {
        this.dbService = dbService;
    }

    public boolean execute(String userIdOrUsername) {
        try {
            MongoDatabase db = dbService.getMongoDatabase();
            MongoCollection<Document> users = db.getCollection("users");
            
            // 尝试通过userId或username查找用户
            Bson filter = eq("userId", userIdOrUsername);
            Document user = users.find(filter).first();
            
            if (user == null) {
                filter = eq("username", userIdOrUsername);
                user = users.find(filter).first();
            }

            if (user == null) {
                LogUtil.warn(BanUserCommand.class, MessageBundle.getMessage("command.ban.notfound", userIdOrUsername));
                return false;
            }

            // 更新用户状态为封禁
            users.updateOne(
                eq("_id", user.get("_id")),
                set("status", "banned")
            );
            
            LogUtil.info(BanUserCommand.class, MessageBundle.getMessage("command.ban.success", userIdOrUsername));
            return true;
        } catch (Exception e) {
            LogUtil.error(BanUserCommand.class, MessageBundle.getMessage("command.ban.error", e.getMessage()));
            return false;
        }
    }

    public boolean unban(String userIdOrUsername) {
        try {
            MongoDatabase db = dbService.getMongoDatabase();
            MongoCollection<Document> users = db.getCollection("users");
            
            // 尝试通过userId或username查找用户
            Bson filter = eq("userId", userIdOrUsername);
            Document user = users.find(filter).first();
            
            if (user == null) {
                filter = eq("username", userIdOrUsername);
                user = users.find(filter).first();
            }

            if (user == null) {
                LogUtil.warn(BanUserCommand.class, MessageBundle.getMessage("command.unban.notfound", userIdOrUsername));
                return false;
            }

            // 更新用户状态为活跃
            users.updateOne(
                eq("_id", user.get("_id")),
                set("status", "active")
            );
            
            LogUtil.info(BanUserCommand.class, MessageBundle.getMessage("command.unban.success", userIdOrUsername));
            return true;
        } catch (Exception e) {
            LogUtil.error(BanUserCommand.class, MessageBundle.getMessage("command.unban.error", e.getMessage()));
            return false;
        }
    }
}
