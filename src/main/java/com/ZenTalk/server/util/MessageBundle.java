package com.ZenTalk.server.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class MessageBundle {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("messages");

    public static String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "???" + key + "???"; // 失败时返回占位符
        }
    }

    public static String getMessage(String key, Object... args) {
        try {
            String pattern = bundle.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return "???" + key + "???"; // 失败时返回占位符
        }
    }
}