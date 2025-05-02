package com.ZenTalk.server.db;

import com.ZenTalk.server.config.Config;
import com.ZenTalk.server.util.LogUtil;
import com.ZenTalk.server.util.MessageBundle;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import com.ZenTalk.repository.UserRepository;

import java.time.Duration;

public class DatabaseService {
    private final Config config;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private JedisPool jedisPool;
    private UserRepository userRepository;

    public DatabaseService(Config config) {
        this.config = config;
        initMongoDB();
        initRedis();
        initRepositories();
        initIndexes();
        scheduleDataCleanup();
    }

    private void initMongoDB() {
        try {
            // MongoDB连接池配置
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(config.mongoUri))
                .serverApi(ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build())
                .applyToConnectionPoolSettings(builder -> 
                    builder.maxSize(50)  // 最大连接数
                        .minSize(5)      // 最小连接数
                        .maxWaitTime(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS) // 最大等待时间
                        .maxConnectionLifeTime(Duration.ofMinutes(30).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS) // 连接最大生命周期
                        .maxConnectionIdleTime(Duration.ofMinutes(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS) // 空闲连接最大生命周期
                )
                .applyToSocketSettings(builder ->
                    builder.connectTimeout((int) Duration.ofSeconds(3).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS) // 连接超时
                    .readTimeout((int) Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)      // 读取超时
                )
                .build();

            this.mongoClient = MongoClients.create(settings);
            this.mongoDatabase = mongoClient.getDatabase(config.mongoDb);
            
            // 验证连接
            this.mongoDatabase.runCommand(new org.bson.Document("ping", 1));
            LogUtil.info(DatabaseService.class, MessageBundle.getMessage("db.mongo.success"));
        } catch (Exception e) {
            LogUtil.error(DatabaseService.class, MessageBundle.getMessage("db.mongo.failure", e.getMessage()));
            throw new RuntimeException(MessageBundle.getMessage("db.mongo.init.exception"), e);
        }
    }

    private void initRedis() {
        try {
            // Redis连接池配置
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(50);         // 最大连接数
            poolConfig.setMaxIdle(10);          // 最大空闲连接数
            poolConfig.setMinIdle(5);           // 最小空闲连接数
            poolConfig.setMaxWaitMillis(3000);  // 最大等待时间
            poolConfig.setTestOnBorrow(true);   // 取连接时进行测试
            poolConfig.setTestOnReturn(true);   // 返还连接时进行测试
            poolConfig.setTestWhileIdle(true);  // 空闲时进行测试
            poolConfig.setTimeBetweenEvictionRunsMillis(30000); // 空闲连接检测周期

            // 只在密码不为空时才使用密码连接
            String redisPassword = config.getRedisPassword();
            if (redisPassword != null && !redisPassword.isEmpty()) {
                this.jedisPool = new JedisPool(poolConfig, 
                    config.getRedisHost(), 
                    config.getRedisPort(),
                    2000,  // 连接超时
                    redisPassword);
            } else {
                // 不使用密码连接
                this.jedisPool = new JedisPool(poolConfig, 
                    config.getRedisHost(), 
                    config.getRedisPort(),
                    2000);  // 连接超时
            }

            // 验证连接
            try (redis.clients.jedis.Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }
            LogUtil.info(DatabaseService.class, MessageBundle.getMessage("db.redis.success"));
        } catch (Exception e) {
            LogUtil.error(DatabaseService.class, MessageBundle.getMessage("db.redis.failure", e.getMessage()));
            throw new RuntimeException(MessageBundle.getMessage("db.redis.init.exception"), e);
        }
    }

    private void initRepositories() {
        this.userRepository = new UserRepository(mongoDatabase, jedisPool);
    }

    private void initIndexes() {
        try {
            MongoIndexConfig.initializeIndexes(mongoDatabase);
        } catch (Exception e) {
            LogUtil.error(DatabaseService.class, MessageBundle.getMessage("db.mongo.index.failure", e.getMessage()));
            throw e;
        }
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    // 定期清理过期数据
    public void scheduleDataCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    cleanupExpiredData();
                    Thread.sleep(24 * 60 * 60 * 1000); // 每24小时执行一次
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LogUtil.error(DatabaseService.class, MessageBundle.getMessage("db.cleanup.failure", e.getMessage()));
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    private void cleanupExpiredData() {
        // 清理7天前的离线消息
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        mongoDatabase.getCollection("messages")
            .deleteMany(new org.bson.Document("createdAt", 
                new org.bson.Document("$lt", sevenDaysAgo)));
            
        // 清理过期的验证码（Redis自动过期，无需手动清理）
        LogUtil.info(DatabaseService.class, MessageBundle.getMessage("db.cleanup.success"));
    }
}