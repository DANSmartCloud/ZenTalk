package com.ZenTalk.server.ws;

import com.ZenTalk.model.Message;
import com.ZenTalk.protobuf.ChatProtocol.ChatFrame;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays; // Added import for Arrays

public class MessageValidator {
    private static final int MAX_TEXT_LENGTH = 5000;
    private static final int MAX_FILE_SIZE_MB = 50;
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("jpg", "jpeg", "png", "gif"); // Use Arrays.asList
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList("mp4", "mov", "avi"); // Use Arrays.asList
    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList("mp3", "wav", "ogg"); // Use Arrays.asList

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }

    public static ValidationResult validate(Message message) {
        List<String> errors = new ArrayList<>();

        // 基础字段验证
        if (message.getFromUser() == null || message.getFromUser().isEmpty()) {
            errors.add("发送者不能为空");
        }
        if (message.getToUser() == null || message.getToUser().isEmpty()) {
            errors.add("接收者不能为空");
        }
        if (message.getContent() == null) {
            errors.add("消息内容不能为null");
        }

        // 根据消息类型进行特定验证
        switch (message.getType()) {
            case TEXT:
                validateTextMessage(message, errors);
                break;
            case IMAGE:
                validateImageMessage(message, errors);
                break;
            case VIDEO:
                validateVideoMessage(message, errors);
                break;
            case AUDIO:
                validateAudioMessage(message, errors);
                break;
            case FILE:
                validateFileMessage(message, errors);
                break;
            case EMOJI:
                validateEmojiMessage(message, errors);
                break;
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private static void validateTextMessage(Message message, List<String> errors) {
        if (message.getContent().length() > MAX_TEXT_LENGTH) {
            errors.add("文本消息长度不能超过 " + MAX_TEXT_LENGTH + " 字符");
        }
        if (message.getContent().trim().isEmpty()) {
            errors.add("文本消息不能为空");
        }
    }

    private static void validateImageMessage(Message message, List<String> errors) {
        validateMediaType(message, errors, ALLOWED_IMAGE_TYPES, "图片");
        validateFileSize(message, errors);
        
        // 验证图片尺寸
        Map<String, String> metadata = message.getMetadata();
        if (metadata != null) {
            try {
                int width = Integer.parseInt(metadata.getOrDefault("width", "0"));
                int height = Integer.parseInt(metadata.getOrDefault("height", "0"));
                if (width <= 0 || height <= 0) {
                    errors.add("图片尺寸无效");
                }
            } catch (NumberFormatException e) {
                errors.add("图片尺寸格式错误");
            }
        }
    }

    private static void validateVideoMessage(Message message, List<String> errors) {
        validateMediaType(message, errors, ALLOWED_VIDEO_TYPES, "视频");
        validateFileSize(message, errors);
        
        // 验证视频时长
        Map<String, String> metadata = message.getMetadata();
        if (metadata != null) {
            try {
                int duration = Integer.parseInt(metadata.getOrDefault("duration", "0"));
                if (duration <= 0) {
                    errors.add("视频时长无效");
                }
            } catch (NumberFormatException e) {
                errors.add("视频时长格式错误");
            }
        }
    }

    private static void validateAudioMessage(Message message, List<String> errors) {
        validateMediaType(message, errors, ALLOWED_AUDIO_TYPES, "音频");
        validateFileSize(message, errors);
    }

    private static void validateFileMessage(Message message, List<String> errors) {
        validateFileSize(message, errors);
        
        // 验证文件名
        Map<String, String> metadata = message.getMetadata();
        if (metadata == null || !metadata.containsKey("fileName")) {
            errors.add("文件名不能为空");
        }
    }

    private static void validateEmojiMessage(Message message, List<String> errors) {
        if (message.getContent().length() > 10) {
            errors.add("表情符号长度过长");
        }
    }

    private static void validateMediaType(Message message, List<String> errors, 
                                       List<String> allowedTypes, String mediaType) {
        String fileName = message.getContent().toLowerCase();
        boolean validType = allowedTypes.stream()
            .anyMatch(type -> fileName.endsWith("." + type));
        if (!validType) {
            errors.add(mediaType + "格式不支持，支持的格式: " + String.join(", ", allowedTypes));
        }
    }

    private static void validateFileSize(Message message, List<String> errors) {
        Map<String, String> metadata = message.getMetadata();
        if (metadata != null && metadata.containsKey("fileSize")) {
            try {
                long fileSize = Long.parseLong(metadata.get("fileSize"));
                long maxSize = MAX_FILE_SIZE_MB * 1024 * 1024L;
                if (fileSize > maxSize) {
                    errors.add("文件大小不能超过 " + MAX_FILE_SIZE_MB + "MB");
                }
            } catch (NumberFormatException e) {
                errors.add("文件大小格式错误");
            }
        }
    }
}