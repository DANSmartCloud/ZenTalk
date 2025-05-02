package com.ZenTalk.server.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtil {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static boolean debugEnabled = true;

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static void info(Class<?> clazz, String message) {
        log(MessageBundle.getMessage("log.level.info"), clazz, message, GREEN);
    }

    public static void warn(Class<?> clazz, String message) {
        log(MessageBundle.getMessage("log.level.warn"), clazz, message, YELLOW);
    }

    public static void error(Class<?> clazz, String message) {
        log(MessageBundle.getMessage("log.level.error"), clazz, message, RED);
    }

    public static void error(Class<?> clazz, String message, Throwable t) {
        log(MessageBundle.getMessage("log.level.error"), clazz, message, RED);
        t.printStackTrace();
    }

    public static void debug(Class<?> clazz, String message) {
        if (debugEnabled) {
            log(MessageBundle.getMessage("log.level.debug"), clazz, message, BLUE);
        }
    }

    private static void log(String level, Class<?> clazz, String message, String color) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = String.format("[%s]<%s>{%s%s%s} %s", 
            timestamp, clazz.getSimpleName(), color, level, RESET, message);
        System.out.println(logMessage);
    }
}
