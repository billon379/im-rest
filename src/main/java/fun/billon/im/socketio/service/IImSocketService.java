package fun.billon.im.socketio.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import fun.billon.mq.api.message.ImChatMessage;
import fun.billon.mq.api.message.ImEventMessage;

/**
 * im消息处理类
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
public interface IImSocketService {

    /**
     * 客户端建立连接
     * 1)获取uuid及用户uid
     * 2)将uuid及uid的关系保存到uidMap和uuidMap
     * 3)向群组发送用户上线事件
     *
     * @param client 客户端连接
     */
    void onConnect(SocketIOClient client);

    /**
     * 客户端连接断开
     * 1)删除uidMap和uuidMap中的uuid及uid的关系
     * 2)向群组发送用户离线事件
     *
     * @param client 客户端连接
     */
    void onDisconnect(SocketIOClient client);

    /**
     * 客户端向群组发送消息
     * 1)解析群组id
     * 2)将消息发送到消息队列
     *
     * @param client        客户端连接
     * @param request       消息应答
     * @param imChatMessage imChatMessage.sender  消息发送者
     *                      imChatMessage.groupId 群组id
     *                      imChatMessage.type  消息类型(1:文本消息;2:图片消息;3:音频消息;4:视频消息)
     *                      imChatMessage.content  消息正文
     *                      imChatMessage.attachment  消息附件
     */
    void onGroupMessageEvent(SocketIOClient client, AckRequest request, ImChatMessage imChatMessage);

    /**
     * 处理消息队列中的Im事件
     * 1)从消息队列中获取事件
     * 2)解析群组id,获取群组用户
     * 3)从uidMap中获取SocketIOClient的UUID,将事件消息发送到客户端
     *
     * @param imEventMessage imEventMessage.type  事件类型(1:群组更新;2:群组解散;3:移除用户;4:用户加入;5:用户离开;6:用户更新)
     *                       imEventMessage.groupId  群组id
     *                       imEventMessage.uid  群组用户uid
     *                       imEventMessage.targetUids 要通知的目标用户列表,群组解散消息需要使用该字段,因为群组已经解散,消费方无法根据群组获取用户
     */
    void processEvent(ImEventMessage imEventMessage);

    /**
     * 处理消息队列中的聊天消息
     * 1)从消息队列中获取聊天消息
     * 2)解析消息类型,如果是群消息,则将获取群组用户;否则直接获取接收者id
     * 3)从uidMap中获取SocketIOClient的UUID,将聊天消息发送到客户端
     *
     * @param imChatMessage imChatMessage.sender  消息发送者
     *                      imChatMessage.groupId 群组id
     *                      imChatMessage.type  消息类型(1:文本消息;2:图片消息;3:音频消息;4:视频消息)
     *                      imChatMessage.content  消息正文
     *                      imChatMessage.attachment  消息附件
     */
    void processMessage(ImChatMessage imChatMessage);

}