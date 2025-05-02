package com.ZenTalk.server.ws;

import com.ZenTalk.model.Message;
import com.ZenTalk.server.chat.ChatHandler; // Import ChatHandler
import com.ZenTalk.service.MessageService; // Import MessageService
import com.ZenTalk.repository.MessageRepository; // Import MessageRepository
import com.ZenTalk.server.db.DatabaseService; // Import DatabaseService
import com.ZenTalk.server.config.Config;
import com.ZenTalk.server.util.LogUtil;
import com.ZenTalk.protobuf.ChatProtocol.ChatFrame;
import com.ZenTalk.server.security.JWTSecretManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

// 移除Jetty相关导入
// import org.eclipse.jetty.server.Server;
// import org.eclipse.jetty.servlet.ServletContextHandler;
// import org.eclipse.jetty.servlet.ServletHolder;
// import org.eclipse.jetty.websocket.api.Session;
// import org.eclipse.jetty.websocket.api.WebSocketListener;
// import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
// import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
// import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

// 添加Tyrus相关导入
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.DebugContext;

import java.time.Duration; // Added import for Duration

import java.io.IOException;
import java.nio.ByteBuffer; // Added import for ByteBuffer
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List; // Added import for List
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors; // Added import for Executors
import java.util.concurrent.ScheduledExecutorService; // Added import for ScheduledExecutorService
import java.util.concurrent.TimeUnit; // Added import for TimeUnit
import java.util.concurrent.atomic.AtomicBoolean;

// 标准WebSocket API
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.PathParam;

public class WebSocketServer {

    private static Server server;
    private static final Set<Session> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<Session, String> sessionUsers = new ConcurrentHashMap<>(); // Map Session to userId
    private static final Map<String, Session> userSessions = new ConcurrentHashMap<>(); // Map userId to Session
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long HEARTBEAT_INTERVAL = 30; // seconds
    private static final long SESSION_TIMEOUT = 90; // seconds
    private static final Map<Session, Long> lastHeartbeat = new ConcurrentHashMap<>();

    // Static fields for dependencies
    private static ChatHandler chatHandler;
    private static MessageAckManager messageAckManager;

    // 不再使用静态初始化块，改为通过startServer方法传入依赖
    // static {
    //     // 依赖项将通过startServer方法传入
    // }

    public static void startServer(Config config, JWTSecretManager secretManager, ChatHandler handler, MessageAckManager ackManager) throws Exception {
        // 设置依赖
        chatHandler = handler;
        messageAckManager = ackManager;
        
        // 获取端口
        int port = config.getWsPort();
        
        // 创建ServerEndpointConfig，包含认证和实例创建逻辑
        ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder.create(UserWebSocketListener.class, "/chat")
            .configurator(new ServerEndpointConfig.Configurator() {
                @Override
                public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
                    // 身份验证逻辑
                    List<String> authHeaders = request.getHeaders().get("Authorization");
                    if (authHeaders == null || authHeaders.isEmpty()) {
                        LogUtil.warn(WebSocketServer.class, "Authentication failed: Missing Authorization header");
                        sec.getUserProperties().put("auth_failed", "Missing Authorization header");
                        return;
                    }
                    String token = authHeaders.get(0).replace("Bearer ", "");
                    try {
                        JwtParser parser = Jwts.parserBuilder()
                                .setSigningKey(JWTSecretManager.getSecretKey())
                                .build();
                        Jws<Claims> claimsJws = parser.parseClaimsJws(token);
                        Claims claims = claimsJws.getBody();
                        String userId = claims.getSubject();
                        sec.getUserProperties().put("userId", userId);
                        LogUtil.info(WebSocketServer.class, "Authentication successful for user: " + userId);
                    } catch (Exception e) {
                        LogUtil.warn(WebSocketServer.class, "Authentication failed: Invalid token - " + e.getMessage());
                        sec.getUserProperties().put("auth_failed", "Invalid token");
                    }
                }

                @Override
                public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                    // 传递依赖项到端点实例
                    if (endpointClass == UserWebSocketListener.class) {
                        return (T) new UserWebSocketListener(chatHandler, messageAckManager);
                    }
                    throw new InstantiationException("Unsupported endpoint class: " + endpointClass.getName());
                }
            })
            .decoders(List.of(MessageDecoder.class))
            .encoders(List.of(MessageEncoder.class))
            .build();
            
        // 使用Tyrus实现WebSocket服务器
        // 创建Server实例，传入主机、端口、上下文路径和端点配置
        Map<String, Object> serverProperties = new HashMap<>();
        // 设置最大消息大小和超时时间
        serverProperties.put(TyrusWebSocketEngine.INCOMING_BUFFER_SIZE, 16384); // 16KB
        serverProperties.put("org.glassfish.tyrus.websockets.timeout", SESSION_TIMEOUT); // 秒
        
        // 创建Server实例
        server = new Server("localhost", port, "/", serverProperties);

        // 启动心跳检查器
        startHeartbeatChecker();

        // 启动服务器
        server.start();
        LogUtil.info(WebSocketServer.class, "WebSocket Server started on port " + port);
    }

    public static void stopServer() throws Exception {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        if (server != null) {
            server.stop();
            LogUtil.info(WebSocketServer.class, "WebSocket Server stopped.");
        }
    }

    private static void startHeartbeatChecker() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            sessions.forEach(session -> {
                if (!session.isOpen()) {
                    sessions.remove(session);
                    lastHeartbeat.remove(session);
                    String userId = sessionUsers.remove(session);
                    if (userId != null) {
                        userSessions.remove(userId);
                        chatHandler.handleClose(session, 1001, "Client disconnected or timed out"); // Notify ChatHandler
                    }
                    return;
                }
                long lastSeen = lastHeartbeat.getOrDefault(session, now);
                if (now - lastSeen > TimeUnit.SECONDS.toMillis(SESSION_TIMEOUT)) {
                    LogUtil.warn(WebSocketServer.class, "Session timeout for user: " + sessionUsers.get(session) + ". Closing session.");
                    try {
                        session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Session timeout"));
                    } catch (IOException e) {
                        LogUtil.error(WebSocketServer.class, "Error closing timed out session: " + e.getMessage());
                    }
                    // Removal logic is handled in onClose
                } else if (now - lastSeen > TimeUnit.SECONDS.toMillis(HEARTBEAT_INTERVAL)) {
                    // Send ping if idle
                    try {
                        // 使用标准WebSocket API发送ping
                        session.getBasicRemote().sendPing(ByteBuffer.wrap("ping".getBytes()));
                    } catch (IOException e) {
                        LogUtil.error(WebSocketServer.class, "Error sending ping: " + e.getMessage());
                    }
                }
            });
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    // Method to get userId from session
    public static String getSessionUser(Session session) {
        return sessionUsers.get(session);
    }

    // Method to get session from userId
    public static Session getUserSession(String userId) {
        return userSessions.get(userId);
    }
    
    // Method to check if user is online
    public static boolean isUserOnline(String userId) {
        Session session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    // Send message to a specific user
    public static void sendMessage(String userId, Message message) {
        Session session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                // Encode Message to ChatFrame before sending
                ChatFrame frame = MessageEncoder.encode(message);
                session.getBasicRemote().sendObject(frame); // 使用标准API
                LogUtil.info(WebSocketServer.class, "Sent message to user " + userId + ": Type=" + message.getType());
            } catch (EncodeException | IOException e) { // 捕获标准异常
                LogUtil.error(WebSocketServer.class, "Error sending message to user " + userId + ": " + e.getMessage());
            }
        } else {
            LogUtil.warn(WebSocketServer.class, "User " + userId + " is not online. Message not sent.");
            // 可选择处理离线消息队列
            chatHandler.handleOfflineMessage(userId, message);
        }
    }

    // Broadcast message to all connected users (excluding sender if needed)
    public static void broadcastMessage(Message message, String senderUserId) {
        ChatFrame frame = MessageEncoder.encode(message);
        sessions.forEach(session -> {
            String recipientUserId = sessionUsers.get(session);
            // Avoid sending to self if senderUserId is provided and matches
            if (session.isOpen() && (senderUserId == null || !senderUserId.equals(recipientUserId))) {
                try {
                    session.getBasicRemote().sendObject(frame); // 使用标准API
                } catch (EncodeException | IOException e) { // 捕获标准异常
                    LogUtil.error(WebSocketServer.class, "Error broadcasting message to user " + recipientUserId + ": " + e.getMessage());
                }
            }
        });
        LogUtil.info(WebSocketServer.class, "Broadcast message: Type=" + message.getType() + (senderUserId != null ? " from user " + senderUserId : ""));
    }

    // 使用标准javax.websocket API的WebSocket事件处理内部类
    @javax.websocket.server.ServerEndpoint("/chat")
    public static class UserWebSocketListener {

        private ChatHandler listenerChatHandler;
        private MessageAckManager listenerMessageAckManager;
        private Session userSession;
        private String userId;

        // 接收依赖项的构造函数
        public UserWebSocketListener(ChatHandler chatHandler, MessageAckManager messageAckManager) {
            this.listenerChatHandler = chatHandler;
            this.listenerMessageAckManager = messageAckManager;
            LogUtil.info(UserWebSocketListener.class, "UserWebSocketListener instance created.");
        }

        @OnOpen
        public void onOpen(Session session, EndpointConfig config) {
            this.userSession = session;
            // 检查配置器中的身份验证结果
            String authFailedReason = (String) config.getUserProperties().get("auth_failed");
            if (authFailedReason != null) {
                LogUtil.warn(UserWebSocketListener.class, "WebSocket connection rejected for session " + session.getId() + ": " + authFailedReason);
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication failed: " + authFailedReason));
                } catch (IOException e) {
                    LogUtil.error(UserWebSocketListener.class, "Error closing rejected session: " + e.getMessage());
                }
                return;
            }

            this.userId = (String) config.getUserProperties().get("userId");
            if (this.userId == null) {
                 LogUtil.warn(UserWebSocketListener.class, "WebSocket connection rejected for session " + session.getId() + ": User ID not found after handshake.");
                 try {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication failed: User ID missing"));
                 } catch (IOException e) {
                    LogUtil.error(UserWebSocketListener.class, "Error closing session with missing user ID: " + e.getMessage());
                 }
                 return;
            }

            sessions.add(session);
            sessionUsers.put(session, userId);
            userSessions.put(userId, session);
            lastHeartbeat.put(session, System.currentTimeMillis());
            LogUtil.info(UserWebSocketListener.class, "WebSocket connection opened: User " + userId + ", Session " + session.getId());
            listenerChatHandler.handleConnect(session, userId); // Notify ChatHandler
        }

        @OnMessage
        public void onMessage(Session session, ChatFrame frame) {
            lastHeartbeat.put(session, System.currentTimeMillis());

            // 根据帧类型处理
            switch (frame.getType()) {
                // 心跳处理使用IMAGE类型，因为MessageType枚举中没有PING
                case IMAGE: // 处理心跳
                    // 已收到心跳，已更新最后心跳时间
                    try {
                        // 使用标准API发送pong
                        session.getBasicRemote().sendPong(ByteBuffer.wrap("pong".getBytes()));
                    } catch (IOException e) {
                         LogUtil.error(UserWebSocketListener.class, "Error sending pong to user " + userId + ": " + e.getMessage());
                    }
                    break;
                case TEXT:
                    try {
                        Message message = MessageDecoder.fromChatFrame(frame); // 将帧解码为消息
                        LogUtil.info(UserWebSocketListener.class, "Received TEXT message from user " + userId + ": Content='" + message.getContent() + "'");
                        listenerChatHandler.handleMessage(session, message);
                    } catch (Exception e) {
                        LogUtil.error(UserWebSocketListener.class, "Error decoding message from user " + userId + ": " + e.getMessage());
                    }
                    break;
                case COMMAND:
                    try {
                        String command = frame.getPayload().toStringUtf8(); // 假设payload是命令
                        LogUtil.info(UserWebSocketListener.class, "Received COMMAND from user " + userId + ": " + command);
                        // 处理命令，例如ACK或HISTORY_REQUEST
                        if (command.startsWith("ACK:")) {
                            String ackMessageId = command.substring(4);
                            listenerMessageAckManager.handleAck(userId, ackMessageId);
                        } else if (command.equals("HISTORY_REQUEST")) {
                            listenerChatHandler.handleHistoryRequest(session, userId);
                        }
                    } catch (Exception e) {
                        LogUtil.error(UserWebSocketListener.class, "Error processing command from user " + userId + ": " + e.getMessage());
                    }
                    break;
                default:
                    LogUtil.warn(UserWebSocketListener.class, "Received unknown frame type from user " + userId + ": " + frame.getType());
                    break;
            }
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            sessions.remove(session);
            lastHeartbeat.remove(session);
            String removedUserId = sessionUsers.remove(session);
            if (removedUserId != null) {
                userSessions.remove(removedUserId);
                LogUtil.info(UserWebSocketListener.class, "WebSocket connection closed: User " + removedUserId + ", Session " + session.getId() +
                        ", Reason: " + closeReason.getReasonPhrase() + " (" + closeReason.getCloseCode().getCode() + ")");
                listenerChatHandler.handleClose(session, closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase()); // Notify ChatHandler
            } else {
                 LogUtil.info(UserWebSocketListener.class, "WebSocket connection closed: Session " + session.getId() +
                        ", Reason: " + closeReason.getReasonPhrase() + " (" + closeReason.getCloseCode().getCode() + ") - User ID was not mapped.");
            }
        }

        @OnError
        public void onError(Session session, Throwable throwable) {
            // Don't remove session here, onClose will handle it
            String errorUserId = sessionUsers.get(session);
            LogUtil.error(UserWebSocketListener.class, "WebSocket error for user " + (errorUserId != null ? errorUserId : "<unknown>") +
                    ", Session " + session.getId() + ": " + throwable.getMessage());
            listenerChatHandler.handleError(session, throwable); // Notify ChatHandler

            // Attempt to close the session gracefully if it's still open
            if (session != null && session.isOpen()) {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Server error"));
                } catch (IOException e) {
                    LogUtil.error(UserWebSocketListener.class, "Error closing session after error: " + e.getMessage());
                }
            }
            // Session removal and cleanup is primarily handled in onClose
        }
    }
}
