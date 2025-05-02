package com.ZenTalk.server.ws;

import com.ZenTalk.model.Message;
import com.ZenTalk.protobuf.ChatProtocol.ChatFrame;
import com.ZenTalk.protobuf.ChatProtocol.MessageType;
import com.ZenTalk.server.util.LogUtil;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息解码器，将ChatFrame对象解码为Message对象
 */
public class MessageDecoder implements Decoder.Binary<ChatFrame> {

    @Override
    public void init(EndpointConfig config) {
        // 初始化解码器，如果需要的话
    }

    @Override
    public void destroy() {
        // 清理资源，如果需要的话
    }

    @Override
    public ChatFrame decode(ByteBuffer bytes) throws DecodeException {
        try {
            // 将字节数组解析为ChatFrame对象
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            return ChatFrame.parseFrom(array);
        } catch (Exception e) {
            throw new DecodeException(bytes, "Error decoding ChatFrame: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean willDecode(ByteBuffer bytes) {
        // 检查是否可以解码
        return bytes != null && bytes.remaining() > 0;
    }

    /**
     * 将ChatFrame对象转换为Message对象
     * @param frame ChatFrame对象
     * @return Message对象
     */
    public static Message fromChatFrame(ChatFrame frame) {
        Message.Builder messageBuilder = new Message.Builder();
        
        // 从payload中解析JSON数据
        String payloadStr = frame.getPayload().toStringUtf8();
        
        // 简单解析JSON字符串，提取必要的字段
        // 注意：这里使用简单的字符串处理，实际项目中应该使用JSON库
        Map<String, String> jsonFields = parseJsonPayload(payloadStr);
        
        // 设置消息ID
        String messageId = jsonFields.get("messageId");
        if (messageId != null && !messageId.isEmpty()) {
            messageBuilder.withId(messageId);
        }
        
        // 设置发送者ID
        String senderId = jsonFields.get("fromUser");
        if (senderId != null && !senderId.isEmpty()) {
            messageBuilder.withFromUser(senderId);
        }
        
        // 设置接收者ID
        String receiverId = jsonFields.get("toUser");
        if (receiverId != null && !receiverId.isEmpty()) {
            messageBuilder.withToUser(receiverId);
        }
        
        // 设置聊天ID
        String chatId = jsonFields.get("chatId");
        if (chatId != null && !chatId.isEmpty()) {
            messageBuilder.withChatId(chatId);
        }
        
        // 设置消息内容
        String content = jsonFields.get("content");
        if (content != null) {
            messageBuilder.withContent(content);
        }
        
        // 根据帧类型设置消息类型
        switch (frame.getType()) {
            case TEXT:
                messageBuilder.withType(Message.MessageType.TEXT);
                break;
            case COMMAND:
                messageBuilder.withType(Message.MessageType.SYSTEM);
                break;
            default:
                LogUtil.warn(MessageDecoder.class, "Unknown frame type: " + frame.getType());
                messageBuilder.withType(Message.MessageType.TEXT);
                break;
        }
        
        // 设置时间戳
        messageBuilder.withStatus(Message.MessageStatus.SENT);
        if (frame.getTimestamp() > 0) {
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(frame.getTimestamp()),
                    ZoneId.systemDefault());
            messageBuilder.withCreatedAt(createdAt);
        } else {
            messageBuilder.withCreatedAt(LocalDateTime.now());
        }
        
        return messageBuilder.build();
    }
    
    /**
     * 简单解析JSON字符串，提取字段值
     * @param jsonStr JSON字符串
     * @return 字段名和值的映射
     */
    private static Map<String, String> parseJsonPayload(String jsonStr) {
        Map<String, String> result = new HashMap<>();
        if (jsonStr == null || jsonStr.isEmpty()) {
            return result;
        }
        
        try {
            // 移除花括号
            String content = jsonStr.trim();
            if (content.startsWith("{")) {
                content = content.substring(1);
            }
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
            
            // 分割字段
            String[] fields = content.split(",");
            for (String field : fields) {
                String[] keyValue = field.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    
                    // 移除引号
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    result.put(key, value);
                }
            }
        } catch (Exception e) {
            LogUtil.error(MessageDecoder.class, "Error parsing JSON payload: " + e.getMessage());
        }
        
        return result;
    }
}
