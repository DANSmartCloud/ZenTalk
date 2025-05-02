package com.ZenTalk.server.chat;

import com.ZenTalk.server.util.AudioUtil;
import com.ZenTalk.server.util.MessageBundle;

public class NotificationHandler {
    // 新消息通知、验证消息通知（带提示音）
    public void notifyNewMessage(String toUser, String message) {
        AudioUtil.playNotificationSound();
        System.out.println(MessageBundle.getMessage("notification.new.message", toUser, message));
    }
    public void notifyVerification(String toUser, String message) {
        AudioUtil.playNotificationSound();
        System.out.println(MessageBundle.getMessage("notification.verify.message", toUser, message));
    }
}