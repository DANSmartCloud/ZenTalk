package com.ZenTalk.server.chat;

import com.ZenTalk.server.util.MessageBundle;
import java.io.File;
import java.util.List;

public class FileHandler {
    // 发送图片/文件（模拟实现）
    public void sendImages(String fromUser, String toUser, List<File> images) {
        for (File img : images) {
            System.out.println(MessageBundle.getMessage("file.transfer.image", fromUser, toUser, img.getName()));
        }
    }

    public void sendFile(String fromUser, String toUser, File file) {
        System.out.println(MessageBundle.getMessage("file.transfer.file", fromUser, toUser, file.getName()));
    }
}