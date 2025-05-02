package com.ZenTalk.server.user;

import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.util.StringResources;
import com.ZenTalk.server.util.LogUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;

public class RegisterHandler {
    private final DatabaseService dbService;

    public RegisterHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    public boolean sendEmailCode(String email, String code) {
        final String from = StringResources.getString("email.from");
        final String password = StringResources.getString("email.password");
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.qq.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(StringResources.getString("email.register.subject"));
            message.setText(StringResources.getString("email.register.content", code));
            Transport.send(message);
            return true;
        } catch (Exception e) {
            LogUtil.error(RegisterHandler.class, StringResources.getString("email.send.failure") + ": " + e.getMessage());
            return false;
        }
    }

    // 注册新用户（仅支持QQ邮箱，需邮箱验证码校验）
    public String register(String username, String password, String email, String phone) {
        if (!email.endsWith("@qq.com")) {
            return StringResources.getString("register.error.qq_only");
        }
        MongoDatabase db = dbService.getMongoDatabase();
        MongoCollection<Document> users = db.getCollection("users");
        if (users.find(Filters.or(
                Filters.eq("username", username),
                Filters.eq("email", email),
                Filters.eq("phone", phone)
            )).first() != null) {
            return null;
        }
        MongoCollection<Document> counter = db.getCollection("uid_counter");
        Document seq = counter.findOneAndUpdate(
            new Document("_id", "uid"),
            new Document("$inc", new Document("seq", 1)),
            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
        );
        long uid = ((Number)seq.get("seq")).longValue();
        String hashedPwd = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
        Document user = new Document("userId", String.valueOf(uid))
            .append("username", username)
            .append("password", hashedPwd)
            .append("email", email)
            .append("phone", phone)
            .append("nickname", username)
            .append("avatar", "default.png")
            .append("createdAt", System.currentTimeMillis());
        users.insertOne(user);
        return String.valueOf(uid);
    }
}