package com.ZenTalk.server.ws;

import com.ZenTalk.model.Message;
import com.ZenTalk.protobuf.ChatProtocol.ChatFrame;
import com.ZenTalk.protobuf.ChatProtocol.MessageType;
import com.google.protobuf.ByteString;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import java.nio.ByteBuffer;
import java.time.ZoneOffset;

/**
 * 消息编码器，将Message对象编码为ChatFrame对象
 */
public class MessageEncoder implements Encoder.Binary<ChatFrame> {

    @Override
    public void init(EndpointConfig config) {
        // 初始化编码器，如果需要的话
    }

    @Override
    public void destroy() {
        // 清理资源，如果需要的话
    }

    @Override
    public ByteBuffer encode(ChatFrame frame) throws EncodeException {
        try {
            // 将ChatFrame对象序列化为字节数组
            byte[] bytes = frame.toByteArray();
            return ByteBuffer.wrap(bytes);
        } catch (Exception e) {
            throw new EncodeException(frame, "Error encoding ChatFrame: " + e.getMessage(), e);
        }
    }

    /**
     * 将Message对象转换为ChatFrame对象
     * @param message 消息对象
     * @return ChatFrame对象
     */
    public static ChatFrame encode(Message message) {
        ChatFrame.Builder frameBuilder = ChatFrame.newBuilder();
        
        // 设置帧类型
        switch (message.getType()) {
            case TEXT:
            case IMAGE:
            case VIDEO:
            case AUDIO:
            case FILE:
            case EMOJI:
                frameBuilder.setType(MessageType.TEXT);
                break;
            case SYSTEM:
                frameBuilder.setType(MessageType.COMMAND);
                break;
            default:
                frameBuilder.setType(MessageType.TEXT);
                break;
        }
        
        // 创建包含所有必要信息的JSON字符串
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{")
            .append("\"messageId\":\"").append(message.getId()).append("\",")
            .append("\"fromUser\":\"").append(message.getFromUser() != null ? message.getFromUser() : "").append("\",")
            .append("\"toUser\":\"").append(message.getToUser() != null ? message.getToUser() : "").append("\",")
            .append("\"chatId\":\"").append(message.getChatId() != null ? message.getChatId() : "").append("\",")
            .append("\"content\":\"").append(message.getContent() != null ? message.getContent().replace("\"", "\\\"") : "").append("\"");
        jsonBuilder.append("}");
        
        // 设置消息内容为JSON字符串
        frameBuilder.setPayload(ByteString.copyFromUtf8(jsonBuilder.toString()));
        
        // 设置时间戳
        frameBuilder.setTimestamp(message.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        
        return frameBuilder.build();
    }
}
