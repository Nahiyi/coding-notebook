package cn.clazs.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WebSocket聊天消息DTO
 * 用于跨服务器的消息传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {

    /**
     * 发送者会话ID
     */
    private String senderSessionId;

    /**
     * 接收者会话ID
     */
    private String receiverSessionId;

    /**
     * 消息内容
     */
    private String content;

}
