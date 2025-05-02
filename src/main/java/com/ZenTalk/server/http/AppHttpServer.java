package com.ZenTalk.server.http;

import com.ZenTalk.server.config.Config;
import com.ZenTalk.server.user.RegisterHandler;
import com.ZenTalk.server.user.LoginHandler;
import com.ZenTalk.server.user.PasswordResetHandler;
import com.ZenTalk.server.user.UserStatusHandler;
import com.ZenTalk.server.chat.ChatHandler;
import com.ZenTalk.server.util.StringResources;
import com.ZenTalk.server.util.LogUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.ZenTalk.server.security.JWTSecretManager; // Import JWTSecretManager

public class AppHttpServer {
    private final Config config;
    private final RegisterHandler registerHandler;
    private final LoginHandler loginHandler;
    private final PasswordResetHandler passwordResetHandler;
    private final UserStatusHandler userStatusHandler;
    private final ChatHandler chatHandler;
    private final JWTSecretManager secretManager; // Add secretManager field
    private HttpServer server;
    private com.ZenTalk.server.ws.WebSocketServer webSocketServer;
    private static final Map<String, String> TEMP_KEYS = new HashMap<>();

    public AppHttpServer(Config config,
                     RegisterHandler registerHandler,
                     LoginHandler loginHandler,
                     PasswordResetHandler passwordResetHandler,
                     UserStatusHandler userStatusHandler,
                     ChatHandler chatHandler,
                     JWTSecretManager secretManager) { // Inject JWTSecretManager
        this.config = config;
        this.registerHandler = registerHandler;
        this.loginHandler = loginHandler;
        this.passwordResetHandler = passwordResetHandler;
        this.userStatusHandler = userStatusHandler;
        this.chatHandler = chatHandler;
        this.secretManager = secretManager; // Use injected secretManager
    }

    public void start() {
        try {
            // Start HTTP server
            server = HttpServer.create(new InetSocketAddress(config.port), 0);
            server.createContext("/ping", new PingHandler());
            server.createContext("/", new IndexHandler());
            server.createContext("/api/register", new ApiRegisterHandler());
            server.createContext("/api/login", new ApiLoginHandler());
            server.createContext("/api/chat", new ApiChatHandler());
            server.createContext("/api/qrcode", new QRCodeHandler());
            server.createContext("/api/test", new ConnectionTestHandler());
            server.createContext("/api/announce", new AnnouncementHandler());
            server.createContext("/api/verify/send", new VerifyCodeSendHandler());
            server.createContext("/api/verify/check", new VerifyCodeCheckHandler());
            server.setExecutor(null);
            ensureIndexHtml();
            server.start();
            LogUtil.info(AppHttpServer.class, "HTTP服务器已启动，监听端口: " + config.port);

            // Start WebSocket server with secret manager
            // webSocketServer = new com.ZenTalk.server.ws.WebSocketServer(); // Instantiation removed as it's static now
            // webSocketServer.start(config, this.secretManager); // Commented out: WebSocket server is started statically in Main
        } catch (IOException e) {
            LogUtil.error(AppHttpServer.class, "HTTP服务器启动失败: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        // if (webSocketServer != null) { // Commented out: WebSocket server is stopped statically in Main
        //     webSocketServer.stop();
        // }
    }

    private void ensureIndexHtml() {
        try {
            File www = new File("www");
            if (!www.exists()) www.mkdir();
            File index = new File(www, "index.html");
            if (!index.exists()) {
                String html = StringResources.getString("websocket.html");
                Files.write(index.toPath(), html.getBytes(), StandardOpenOption.CREATE);
                LogUtil.info(AppHttpServer.class, "已自动生成 www/index.html");
            }
        } catch (Exception e) {
            LogUtil.error(AppHttpServer.class, "生成index.html失败: " + e.getMessage());
        }
    }

    // 以下是各个处理器类的实现
    static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "pong";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class QRCodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                String content = query.substring(query.indexOf('=') + 1);
                
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);
                
                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                byte[] pngData = pngOutputStream.toByteArray();
                
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, pngData.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(pngData);
                }
            } catch (Exception e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "QR_CODE_GENERATION_FAILED");
                exchange.sendResponseHeaders(500, error.toString().getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.toString().getBytes());
                }
            }
        }
    }

    // 其他处理器类实现...
    static class ConnectionTestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            exchange.sendResponseHeaders(200, response.toString().getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.toString().getBytes());
            }
        }
    }

    static class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                File file = new File("www/index.html");
                byte[] fileContent = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, fileContent.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileContent);
                }
            } catch (Exception e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INDEX_FILE_NOT_FOUND");
                exchange.sendResponseHeaders(404, error.toString().getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.toString().getBytes());
                }
            }
        }
    }

    class ApiRegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 注册处理逻辑实现...
        }
    }

    class ApiLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 登录处理逻辑实现...
        }
    }

    class ApiChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 聊天处理逻辑实现...
        }
    }

    class VerifyCodeSendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 验证码发送处理逻辑实现...
        }
    }

    class VerifyCodeCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 验证码校验处理逻辑实现...
        }
    }

    static class AnnouncementHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 公告处理逻辑实现...
        }
    }
}
