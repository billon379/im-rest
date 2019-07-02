package fun.billon.im.socketio;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import fun.billon.im.socketio.service.IImSocketService;
import fun.billon.mq.api.message.ImChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * socketio消息接收器。只做消息的接收，消息的处理交给业务层
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);

    /**
     * 事件类型(group_message):群组消息
     */
    public static final String EVENT_GROUP_MESSAGE = "group_message";

    /**
     * 事件类型(group_event):群组事件
     */
    public static final String EVENT_GROUP_EVENT = "group_event";

    @Resource
    private IImSocketService imSocketService;

    /**
     * 客户端建立连接
     *
     * @param client 客户端连接
     */
    @OnConnect
    public void onConnect(SocketIOClient client) {
        imSocketService.onConnect(client);
    }

    /**
     * 客户端连接断开
     *
     * @param client 客户端连接
     */
    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        imSocketService.onDisconnect(client);
    }

    /**
     * 客户端向群组发送消息
     *
     * @param client        客户端连接
     * @param request       消息应答
     * @param imChatMessage imChatMessage.sender  消息发送者
     *                      imChatMessage.groupId 群组id
     *                      imChatMessage.type  消息类型(1:文本消息;2:图片消息;3:音频消息;4:视频消息)
     *                      imChatMessage.content  消息正文
     *                      imChatMessage.attachment  消息附件
     */
    @OnEvent(value = EVENT_GROUP_MESSAGE)
    public void onGroupMessageEvent(SocketIOClient client, AckRequest request, ImChatMessage imChatMessage) {
        imSocketService.onGroupMessageEvent(client, request, imChatMessage);
    }

}