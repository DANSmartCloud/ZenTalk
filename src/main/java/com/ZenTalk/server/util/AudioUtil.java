package com.ZenTalk.server.util;

import com.ZenTalk.server.util.MessageBundle;
import javax.sound.sampled.*;
import java.io.File;

public class AudioUtil {
    private static final String NOTIFICATION_SOUND_PATH = "sounds/notification.wav";

    public static void playNotificationSound() {
        try {
            File soundFile = new File(NOTIFICATION_SOUND_PATH);
            if (!soundFile.exists()) {
                System.out.println(MessageBundle.getMessage("audio.notification.missing", NOTIFICATION_SOUND_PATH));
                return;
            }
            
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            System.err.println(MessageBundle.getMessage("audio.play.error", e.getMessage()));
        }
    }
}
