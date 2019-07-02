package fun.billon.im.socketio.service.impl;

import com.alibaba.fastjson.JSON;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import fun.billon.common.model.ResultModel;
import fun.billon.im.api.model.ImGroupMemberModel;
import fun.billon.im.api.model.ImGroupModel;
import fun.billon.im.rest.service.IImService;
import fun.billon.im.socketio.EventHandler;
import fun.billon.im.socketio.service.IImSocketService;
import fun.billon.mq.api.message.ImChatMessage;
import fun.billon.mq.api.message.ImEventMessage;
import fun.billon.mq.api.producer.ImChatMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * im消息处理实现类
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class ImSocketServiceImpl implements IImSocketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImSocketServiceImpl.class);

    @Value("${billon.im.sid}")
    private String sid;

    @Resource
    private ImChatMessageProducer imChatMessageProducer;

    @Resource
    private IImService imService;

    /**
     * 用户uid-->SocketIOClient的映射。
     * 注意：同一个用户可以在多个平台登录
     */
    private static final Map<Integer, List<SocketIOClient>> SOCKET_MAPPER = new HashMap<>(200);

    /**
     * 客户端建立连接
     * 1)将uid-->SocketIOClient的关系保存到SOCKET_MAPPER
     * 2)向群组发送用户上线事件
     *
     * @param client 客户端连接
     */
    @Override
    public void onConnect(SocketIOClient client) {
        LOGGER.debug("客户端连接建立[uuid]:{}", client.getSessionId());
        int uid = Integer.valueOf(client.getHandshakeData().getSingleUrlParam("uid"));
        List<SocketIOClient> uidSocketList = SOCKET_MAPPER.get(uid);
        if (uidSocketList == null) {
            uidSocketList = new ArrayList<>(2);
            SOCKET_MAPPER.put(Integer.valueOf(uid), uidSocketList);
        }
        if (!uidSocketList.contains(client)) {
            uidSocketList.add(client);
        }
        /**
         * TODO,发送用户上线事件
         */
    }

    /**
     * 客户端连接断开
     * 1)删除SOCKET_MAPPER中的uid-->SocketIOClient的关系
     * 2)向群组发送用户离线事件
     *
     * @param client 客户端连接
     */
    @Override
    public void onDisconnect(SocketIOClient client) {
        LOGGER.debug("客户端连接断开[uuid]:{}", client.getSessionId());
        int uid = Integer.valueOf(client.getHandshakeData().getSingleUrlParam("uid"));
        List<SocketIOClient> uidSocketList = SOCKET_MAPPER.get(uid);
        if (uidSocketList != null) {
            uidSocketList.remove(client);
            if (uidSocketList.size() == 0) {
                SOCKET_MAPPER.remove(uid);
            }
        }
        /**
         * TODO,发送用户离线事件
         */
    }

    /**
     * 客户端向群组发送消息
     * 1)解析群组id
     * 2)将消息发送到消息队列
     *
     * @param client        客户端连接
     * @param request       消息应答
     * @param imChatMessage imChatMessage.sender  消息发送者
     *                      imChatMessage.groupId  群组id
     *                      imChatMessage.type  消息类型(1:文本消息;2:图片消息;3:音频消息;4:视频消息)
     *                      imChatMessage.content  消息正文
     *                      imChatMessage.attachment  消息附件
     */
    @Override
    public void onGroupMessageEvent(SocketIOClient client, AckRequest request, ImChatMessage imChatMessage) {
        LOGGER.debug("收到客户端{}消息:{}", client.getSessionId(), imChatMessage);
        int uid = Integer.valueOf(client.getHandshakeData().getSingleUrlParam("uid"));
        imChatMessage.setSender(uid);

        /*
         * 检查群组是否存在以及用户是否在群组
         */
        ResultModel resultModel = new ResultModel();
        ResultModel<ImGroupMemberModel> resultModelImGroupMember = imService.groupMember(0, imChatMessage.getGroupId(),
                imChatMessage.getSender());
        if (resultModelImGroupMember.getCode() != ResultModel.RESULT_SUCCESS) {
            resultModel.setFailed(resultModelImGroupMember);
            request.sendAckData(JSON.toJSON(resultModel));
            return;
        }

        /*
         * 将消息发送到MQ
         */
        imChatMessage.setTime(new Date());
        imChatMessageProducer.produce(imChatMessage);
        request.sendAckData(JSON.toJSON(resultModel));
    }

    /**
     * 处理消息队列中的Im事件
     * 1)从消息队列中获取事件
     * 2)解析群组id,获取群组用户
     * 3)从UID_MAPPER中获取SocketIOClient的SocketIOClient,将事件消息发送到客户端
     *
     * @param imEventMessage imEventMessage.type  事件类型(1:群组更新;2:群组解散;3:移除用户;4:用户加入;5:用户离开;6:用户更新)
     *                       imEventMessage.groupId  群组id
     *                       imEventMessage.uid  群组用户uid
     *                       imEventMessage.targetUids 要通知的目标用户列表,群组解散消息需要使用该字段,因为群组已经解散,消费方无法根据群组获取用户
     */
    @Override
    public void processEvent(ImEventMessage imEventMessage) {
        /*
         * 群组解散事件单独处理
         */
        if (imEventMessage.getType() == ImEventMessage.GROUP_DELETE) {
            for (int uid : imEventMessage.getTargetUids()) {
                sendEvent(EventHandler.EVENT_GROUP_EVENT, uid, imEventMessage);
            }
            return;
        }

        /*
         * 其它事件需从群组中获取用户信息
         */
        ImGroupModel imGroupModel = new ImGroupModel(imEventMessage.getGroupId());
        imGroupModel.setRequireMember(true);
        ResultModel<ImGroupModel> resultModelImGroup = imService.group(imEventMessage.getUid(), imGroupModel);
        if (resultModelImGroup.getCode() != ResultModel.RESULT_SUCCESS) {
            return;
        }
        for (ImGroupMemberModel imGroupMemberModel : resultModelImGroup.getData().getMembers()) {
            sendEvent(EventHandler.EVENT_GROUP_EVENT, imGroupMemberModel.getUid(), imEventMessage);
        }
    }

    /**
     * 处理消息队列中的聊天消息
     * 1)从消息队列中获取聊天消息
     * 2)解析消息类型,如果是群消息,则将获取群组用户;否则直接获取接收者id
     * 3)从SOCKET_MAPPER中获取SocketIOClient,将聊天消息发送到客户端
     *
     * @param imChatMessage imChatMessage.sender  消息发送者
     *                      imChatMessage.groupId 群组id
     *                      imChatMessage.type  消息类型(1:文本消息;2:图片消息;3:音频消息;4:视频消息)
     *                      imChatMessage.content  消息正文
     *                      imChatMessage.attachment  消息附件
     */
    @Override
    public void processMessage(ImChatMessage imChatMessage) {
        ImGroupModel imGroupModel = new ImGroupModel(imChatMessage.getGroupId());
        imGroupModel.setRequireMember(true);
        ResultModel<ImGroupModel> resultModelImGroup = imService.group(imChatMessage.getSender(), imGroupModel);
        if (resultModelImGroup.getCode() != ResultModel.RESULT_SUCCESS) {
            return;
        }
        for (ImGroupMemberModel imGroupMemberModel : resultModelImGroup.getData().getMembers()) {
            if (imGroupMemberModel.getUid() != imChatMessage.getSender()) {
                sendEvent(EventHandler.EVENT_GROUP_MESSAGE, imGroupMemberModel.getUid(), imChatMessage);
            }
        }
    }

    /**
     * 向客户端发送消息
     *
     * @param event    事件名称
     * @param receiver 接收用户uid
     * @param message  消息内容
     */
    private void sendEvent(String event, int receiver, Object message) {
        List<SocketIOClient> uidSocketList = SOCKET_MAPPER.get(receiver);
        if (uidSocketList == null || uidSocketList.size() == 0) {
            return;
        }
        for (SocketIOClient socketIOClient : uidSocketList) {
            socketIOClient.sendEvent(event, JSON.toJSON(message));
        }
    }

}