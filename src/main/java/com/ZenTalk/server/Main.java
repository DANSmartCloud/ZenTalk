package com.ZenTalk.server;

import com.ZenTalk.server.config.Config;
import com.ZenTalk.server.config.ConfigLoader;
import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.http.AppHttpServer;
import com.ZenTalk.server.user.RegisterHandler;
import com.ZenTalk.server.user.LoginHandler;
import com.ZenTalk.server.user.PasswordResetHandler;
import com.ZenTalk.server.user.UserStatusHandler;
import com.ZenTalk.server.chat.ChatHandler;
import com.ZenTalk.server.command.CommandHandler;
import com.ZenTalk.server.util.LogUtil;
import com.ZenTalk.server.util.MessageBundle;
import com.ZenTalk.server.ws.WebSocketServer;
import com.ZenTalk.server.ws.MessageAckManager;
import com.ZenTalk.repository.MessageRepository;
import com.ZenTalk.repository.MongoMessageRepository;
import com.ZenTalk.service.MessageService;
import com.ZenTalk.server.security.JWTSecretManager; // Added import for JWTSecretManager

public class Main {
    static {
        // 配置MongoDB日志
        System.setProperty("org.mongodb.driver.log.level", "OFF");
        System.setProperty("org.mongodb.driver.log.path", "");
        
        // 初始化JUL到SLF4J的桥接
        // 移除已存在的JUL处理器
        java.util.logging.LogManager.getLogManager().reset();
        // 安装SLF4J桥接处理器
        org.slf4j.bridge.SLF4JBridgeHandler.install();
    }

    // Declare messageAckManager here
    private static MessageAckManager messageAckManager;

    public static void main(String[] args) {
        LogUtil.info(Main.class, MessageBundle.getMessage("server.welcome"));
        LogUtil.info(Main.class, MessageBundle.getMessage("server.readme"));
        LogUtil.info(Main.class, MessageBundle.getMessage("server.version", "1.5.1"));
        // 加载配置
        LogUtil.info(Main.class, MessageBundle.getMessage("server.load.config"));
        Config config = ConfigLoader.load();
        // 初始化数据库
        LogUtil.info(Main.class, MessageBundle.getMessage("server.init.db"));
        DatabaseService dbService = new DatabaseService(config);
        // 初始化用户相关处理器
        LogUtil.info(Main.class, MessageBundle.getMessage("server.init.user.handlers"));
        RegisterHandler registerHandler = new RegisterHandler(dbService);
        LoginHandler loginHandler = new LoginHandler(dbService);
        PasswordResetHandler passwordResetHandler = new PasswordResetHandler(dbService);
        UserStatusHandler userStatusHandler = new UserStatusHandler(dbService);
        // Initialize chat handler (placeholder, will be re-initialized later)
        // ChatHandler chatHandler = new ChatHandler(dbService);
        // Initialize JWT secret manager
        LogUtil.info(Main.class, MessageBundle.getMessage("server.init.jwt.manager"));
        JWTSecretManager secretManager = new JWTSecretManager(); // Corrected constructor call
        secretManager.init();
        
        // Initialize message repository
        MessageRepository messageRepository = new MongoMessageRepository(
            dbService.getMongoDatabase(), 
            dbService.getJedisPool()
        );

        // Initialize WebSocket server components statically
        // WebSocketServer webSocketServer = new WebSocketServer(); // Removed instantiation
        // webSocketServer.start(config, secretManager); // Removed instance call

        // Initialize message acknowledgment manager (without WebSocketServer instance)
        // Initialize message acknowledgment manager (corrected constructor call)
        messageAckManager = new MessageAckManager(messageRepository);

        // Initialize message service (already corrected)
        MessageService messageService = new MessageService(
            messageRepository
            // webSocketServer // Already removed
        );

        // Initialize ChatHandler with all dependencies
        ChatHandler chatHandler = new ChatHandler(dbService, messageService, messageAckManager, messageRepository);

        // Start WebSocket server statically, passing dependencies
        try {
            WebSocketServer.startServer(config, secretManager, chatHandler, messageAckManager);
        } catch (Exception e) {
            LogUtil.error(Main.class, "Failed to start WebSocket server: " + e.getMessage());
            System.exit(1);
        }

        // Start HTTP server and WebSocket service, inject all handlers and secretManager
        AppHttpServer httpServer = new AppHttpServer(
            config,
            registerHandler,
            loginHandler,
            passwordResetHandler,
            userStatusHandler,
            chatHandler,
            secretManager // Inject secretManager
        );
        httpServer.start();
        
        // 启动命令行控制台
        CommandHandler commandHandler = new CommandHandler(dbService, httpServer);
        commandHandler.listen();
        
        // 添加优雅关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LogUtil.info(Main.class, MessageBundle.getMessage("server.shutdown.start"));
            httpServer.stop();
            // webSocketServer.stop(); // Removed instance call
            try {
                WebSocketServer.stopServer(); // Call static stop method
            } catch (Exception e) {
                LogUtil.error(Main.class, "Error stopping WebSocket server: " + e.getMessage());
            }
            secretManager.shutdown();
            dbService.shutdown();
            LogUtil.info(Main.class, MessageBundle.getMessage("server.shutdown.complete"));
        }));
    }
}
