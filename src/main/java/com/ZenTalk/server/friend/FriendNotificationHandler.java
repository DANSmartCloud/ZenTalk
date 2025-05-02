package com.ZenTalk.server.friend;

import com.ZenTalk.server.util.AudioUtil;
import com.ZenTalk.server.util.MessageBundle;

public class FriendNotificationHandler {
    // 好友相关通知（如验证消息通知，带提示音）
    public void notifyVerification(String toUser, String message) {
        System.out.println(MessageBundle.getMessage("notification.friend.verify", toUser, message));
        AudioUtil.playNotificationSound();
    }
}