package com.ZenTalk.server.config;

import java.security.Key;

public class Config {
    public int port;
    public String ip;
    public boolean allowLogin;
    public boolean allowRegister;
    public String mongoUri;
    public String mongoDb;
    public String redisHost;
    public int redisPort;
    public String redisPassword; // Added redisPassword field
    public String wsHost;
    public int wsPort;
    private transient Key jwtSecret;

    public Config(int port, String ip, boolean allowLogin, boolean allowRegister, 
                 String mongoUri, String mongoDb, String redisHost, int redisPort,
                 String redisPassword, // Added redisPassword to constructor
                 String wsHost, int wsPort, Key jwtSecret) {
        this.port = port;
        this.ip = ip;
        this.allowLogin = allowLogin;
        this.allowRegister = allowRegister;
        this.mongoUri = mongoUri;
        this.mongoDb = mongoDb;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisPassword = redisPassword; // Initialize redisPassword
        this.wsHost = wsHost;
        this.wsPort = wsPort;
        this.jwtSecret = jwtSecret;
    }

    public String getWsHost() {
        return wsHost;
    }

    public int getWsPort() {
        return wsPort;
    }

    public Key getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(Key jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    // Added getter for redisPassword
    public String getRedisPassword() {
        return redisPassword;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }
}