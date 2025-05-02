package com.ZenTalk.server.config;

import io.jsonwebtoken.security.Keys;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    public static Config load() {
        Properties props = new Properties();
        try {
            InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties");
            if (in == null) {
                in = ConfigLoader.class.getClassLoader().getResourceAsStream("config-default.properties");
            }
            if (in == null) {
                throw new RuntimeException("无法加载配置文件");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("无法加载配置文件");
        }
        int port = Integer.parseInt(props.getProperty("server.port", "8080"));
        String ip = props.getProperty("server.ip", "127.0.0.1");
        boolean allowLogin = Boolean.parseBoolean(props.getProperty("allow.login", "true"));
        boolean allowRegister = Boolean.parseBoolean(props.getProperty("allow.register", "true"));
        String mongoUri = props.getProperty("mongo.uri", "mongodb://localhost:27017");
        String mongoDb = props.getProperty("mongo.db", "chat");
        String redisHost = props.getProperty("redis.host", "localhost");
        int redisPort = Integer.parseInt(props.getProperty("redis.port", "6379"));
        String redisPassword = props.getProperty("redis.password", ""); // Load redis password, default to empty
        String wsHost = props.getProperty("websocket.host", "0.0.0.0");
        int wsPort = Integer.parseInt(props.getProperty("websocket.port", "8081"));
        String jwtSecret = props.getProperty("jwt.secret", "default-secret-change-in-production");
        
        return new Config(port, ip, allowLogin, allowRegister, mongoUri, mongoDb, 
                        redisHost, redisPort, redisPassword, // Pass redisPassword
                        wsHost, wsPort, 
                        Keys.hmacShaKeyFor(jwtSecret.getBytes()));
    }
}