package com.ZenTalk.server.util;

import java.util.HashMap;
import java.util.Map;

public class StringResources {
    private static final Map<String, String> STRING_MAP = new HashMap<>();
    
    static {
        // Redis related strings
        STRING_MAP.put("user.status.key", "user:status:");
        STRING_MAP.put("offline.msg.key", "offline:msg:");
        
        // HTTP related strings
        STRING_MAP.put("websocket.html", "<html><head><title>WebSocket Server</title></head><body><h1>WebSocket Server is running</h1></body></html>");
        STRING_MAP.put("pong.response", "pong");
        STRING_MAP.put("not.found.response", "index.html not found.");
        
        // Email related strings
        STRING_MAP.put("email.from", "yourQQemail@qq.com");
        STRING_MAP.put("email.password", "yourEmailPassword");
    }
    
    public static String getString(String key) {
        return STRING_MAP.get(key);
    }
    
    public static String getString(String key, String defaultValue) {
        return STRING_MAP.getOrDefault(key, defaultValue);
    }
}
