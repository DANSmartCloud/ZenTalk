package com.ZenTalk.server.chat;

import com.ZenTalk.server.util.MessageBundle;

public class EmojiHandler {
    // ...其他代码

    public void loadEmoji() {
        try {
            // ...加载emoji的代码
        } catch (Exception e) {
            System.out.println(MessageBundle.getMessage("emoji.load.error"));
        }
    }
}