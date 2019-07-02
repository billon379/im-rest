package fun.billon.im.rest.service.impl;

import fun.billon.common.model.ResultModel;
import fun.billon.common.util.StringUtils;
import fun.billon.im.api.constant.ImStatusCode;
import fun.billon.im.api.model.ImGroupMemberModel;
import fun.billon.im.api.model.ImGroupModel;
import fun.billon.im.rest.dao.IImGroupDAO;
import fun.billon.im.rest.dao.IImGroupMemberDAO;
import fun.billon.im.rest.service.IImService;
import fun.billon.member.api.feign.IMemberService;
import fun.billon.member.api.model.UserModel;
import fun.billon.mq.api.message.ImEventMessage;
import fun.billon.mq.api.producer.ImEventMessageProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import java.util.*;

/**
 * im功能服务层实现
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class ImServiceImpl implements IImService {

    @Value("${billon.im.sid}")
    private String sid;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Resource
    private IImGroupDAO imGroupDAO;

    @Resource
    private IImGroupMemberDAO imGroupMemberDAO;

    @Resource
    private IMemberService memberService;

    @Resource
    private ImEventMessageProducer imEventMessageProducer;

    /**
     * 创建群组
     * 1)向群组表中插入群组信息
     * 2)向群用户表中插入群主信息
     *
     * @param imGroupModel imGroupModel.name 群组名称 必填
     *                     imGroupModel.creatorId 群主id 必填
     *                     imGroupModel.destination 目的地 选填
     *                     imGroupModel.latitude 目的地纬度(gps) 选填
     *                     imGroupModel.longitude 目的地经度(gps) 选填
     * @param nickname     群主昵称 选填
     * @return 群组id
     */
    @Override
    public ResultModel<Integer> createGroup(ImGroupModel imGroupModel, String nickname) {
        ResultModel<Integer> resultModel = new ResultModel<>();
        /*
         * 声明事务
         */
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            /*
             * 1)向群组表中插入群组信息
             */
            // 设置口令
            imGroupModel.setPassword(getAvailablePassword());
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.add(Calendar.DAY_OF_MONTH, ImGroupModel.PASSWORD_EXPIRE_TIME);
            // 设置口令过期时间，7天
            imGroupModel.setPasswordExpireTime(calendar.getTime());
            imGroupModel.setMaxMember(ImGroupModel.MAX_MEMBER);
            imGroupDAO.insertImGroup(imGroupModel);

            /*
             * 2)向群用户表中插入群主信息
             */
            ResultModel<UserModel> resultModelUser = memberService.getUserById(sid, sid, imGroupModel.getCreatorId());
            if (resultModelUser.getCode() != ResultModel.RESULT_SUCCESS) {
                // 获取用户信息失败,回滚事务
                transactionManager.rollback(status);
                return resultModel.setFailed(resultModelUser);
            }
            UserModel um = resultModelUser.getData();
            // 保存群主信息
            imGroupMemberDAO.insertImGroupMember(new ImGroupMemberModel(um.getId().intValue(), imGroupModel.getId(),
                    StringUtils.isEmpty(nickname) ? um.getAccount() : nickname, um.getHeadImgUrl(), ImGroupMemberModel.OWNER));
            transactionManager.commit(status);
            resultModel.setData(imGroupModel.getId());
        } catch (Exception e) {
            transactionManager.rollback(status);
            return resultModel.setFailed(ImStatusCode.IM_DB_EXCEPTION, e.getCause().getMessage());
        }

        return resultModel;
    }

    /**
     * 解散群组
     *
     * @param imGroupModel imGroupModel.id 主键 必填
     *                     imGroupModel.creatorId 群主id 必填
     * @return 执行结果
     */
    @Override
    public ResultModel deleteGroup(ImGroupModel imGroupModel) {
        ResultModel resultModel = new ResultModel();
        // 检查群主信息
        ResultModel resultModelCheckGroupOwner = checkGroupOwner(imGroupModel.getId(), imGroupModel.getCreatorId());
        if (resultModelCheckGroupOwner.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelCheckGroupOwner);
        }
        // 获取群组用户
        ImGroupMemberModel imGroupMemberModel = new ImGroupMemberModel(imGroupModel.getId());
        List<Integer> groupMemberIds = imGroupMemberDAO.queryPKListByCriteria(imGroupMemberModel);
        /*
         * 声明事务
         */
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            List<Integer> groupMemberUids = new ArrayList<>(10);
            // 删除群组用户
            for (Integer id : groupMemberIds) {
                imGroupMemberModel.setId(id);
                groupMemberUids.add(imGroupMemberDAO.queryImGroupMemberByPK(imGroupMemberModel).getUid());
                imGroupMemberDAO.deleteImGroupMemberByPK(imGroupMemberModel);
            }
            // 删除群组
            imGroupDAO.deleteImGroupByPK(imGroupModel);
            transactionManager.commit(status);

            /*
             * 将群组解散消息放入MQ
             */
            ImEventMessage imEventMessage = new ImEventMessage();
            imEventMessage.setGroupId(imGroupModel.getId());
            imEventMessage.setType(ImEventMessage.GROUP_DELETE);
            imEventMessage.setTargetUids(groupMemberUids);
            imEventMessage.setTime(new Date());
            imEventMessageProducer.produce(imEventMessage);
        } catch (Exception e) {
            transactionManager.rollback(status);
            return resultModel.setFailed(ImStatusCode.IM_DB_EXCEPTION, e.getCause().getMessage());
        }
        return resultModel;
    }

    /**
     * 更新群组
     *
     * @param imGroupModel imGroupModel.id 主键 必填
     *                     imGroupModel.creatorId 群主id 必填
     *                     imGroupModel.name 群组名称 选填
     *                     imGroupModel.destination 目的地 选填
     *                     imGroupModel.latitude 目的地纬度(gps) 选填
     *                     imGroupModel.longitude 目的地经度(gps) 选填
     * @param nickname     群主昵称 选填
     * @return 执行结果
     */
    @Override
    public ResultModel updateGroup(ImGroupModel imGroupModel, String nickname) {
        ResultModel resultModel = new ResultModel();
        // 检查群主信息
        ResultModel resultModelCheckGroupOwner = checkGroupOwner(imGroupModel.getId(), imGroupModel.getCreatorId());
        if (resultModelCheckGroupOwner.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelCheckGroupOwner);
        }
        // 更新群组信息
        imGroupDAO.updateImGroupByPK(imGroupModel);

        // 昵称不为空增更新用户昵称
        if (!StringUtils.isEmpty(nickname)) {
            ImGroupMemberModel imGroupMemberModel = new ImGroupMemberModel(imGroupModel.getId());
            imGroupMemberModel.setUid(imGroupModel.getCreatorId());
            Integer id = imGroupMemberDAO.queryPKByCriteria(imGroupMemberModel);
            imGroupMemberModel.setId(id);
            imGroupMemberModel.setNickname(nickname);
            imGroupMemberDAO.updateImGroupMemberByPK(imGroupMemberModel);

            /*
             * 将用户更新消息放入MQ
             */
            ImEventMessage imEventMessage = new ImEventMessage();
            imEventMessage.setGroupId(imGroupModel.getId());
            imEventMessage.setUid(imGroupModel.getCreatorId());
            imEventMessage.setType(ImEventMessage.MEMBER_UPDATE);
            imEventMessage.setTime(new Date());
            imEventMessageProducer.produce(imEventMessage);
        }

        /*
         * 将群组更新消息放入MQ
         */
        ImEventMessage imGroupEventMessage = new ImEventMessage();
        imGroupEventMessage.setGroupId(imGroupModel.getId());
        imGroupEventMessage.setType(ImEventMessage.GROUP_UPDATE);
        imGroupEventMessage.setTime(new Date());
        imEventMessageProducer.produce(imGroupEventMessage);
        return resultModel;
    }

    /**
     * 更新群组口令
     *
     * @param imGroupModel imGroupModel.id 主键 必填
     *                     imGroupModel.creatorId 群主id 必填
     * @return 群组信息
     */
    @Override
    public ResultModel<ImGroupModel> updateGroupPassword(ImGroupModel imGroupModel) {
        ResultModel<ImGroupModel> resultModel = new ResultModel<>();
        // 检查群主信息
        ResultModel resultModelCheckGroupOwner = checkGroupOwner(imGroupModel.getId(), imGroupModel.getCreatorId());
        if (resultModelCheckGroupOwner.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelCheckGroupOwner);
        }
        // 生成群组口令
        imGroupModel.setPassword(getAvailablePassword());
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.add(Calendar.DAY_OF_MONTH, ImGroupModel.PASSWORD_EXPIRE_TIME);
        // 设置口令过期时间，7天
        imGroupModel.setPasswordExpireTime(calendar.getTime());
        imGroupDAO.updateImGroupByPK(imGroupModel);
        resultModel.setData(imGroupDAO.queryImGroupByPK(imGroupModel));

        /*
         * 将群组更新消息放入MQ
         */
        ImEventMessage imEventMessage = new ImEventMessage();
        imEventMessage.setGroupId(imGroupModel.getId());
        imEventMessage.setType(ImEventMessage.GROUP_UPDATE);
        imEventMessage.setTime(new Date());
        imEventMessageProducer.produce(imEventMessage);
        return resultModel;
    }

    /**
     * 移除群组成员
     *
     * @param creatorId 群主id 必填
     * @param groupId   群组id 必填
     * @param uid       用户uid 必填
     * @return 执行结果
     */
    @Override
    public ResultModel removeGroupMember(int creatorId, int groupId, int uid) {
        ResultModel resultModel = new ResultModel();
        // 检查群主信息
        ResultModel resultModelCheckGroupOwner = checkGroupOwner(groupId, creatorId);
        if (resultModelCheckGroupOwner.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelCheckGroupOwner);
        }

        // 检查要移除的是否是群主
        if (creatorId == uid) {
            // 无法移除群主
            return resultModel.setFailed(ImStatusCode.IM_GROUP_OPERATION_FORBIDDEN, "无法移除群主");
        }

        // 查询群组用户信息
        ResultModel<ImGroupMemberModel> resultModelImGroupMember = checkGroupMember(groupId, uid);
        if (resultModelImGroupMember.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelImGroupMember);
        }

        // 删除群组用户
        ImGroupMemberModel imGroupMemberModel = resultModelImGroupMember.getData();
        imGroupMemberDAO.deleteImGroupMemberByPK(imGroupMemberModel);

        // 群组用户数-1
        ImGroupModel imGroupModel = new ImGroupModel(groupId);
        imGroupModel.setCreatorId(creatorId);
        imGroupModel.setMemberCount(-1);
        imGroupDAO.updateImGroupByPK(imGroupModel);

        /*
         * 将移除用户消息放入MQ
         */
        ImEventMessage imEventMessage = new ImEventMessage();
        imEventMessage.setGroupId(groupId);
        imEventMessage.setUid(uid);
        imEventMessage.setType(ImEventMessage.REMOVE_MEMBER);
        imEventMessage.setTime(new Date());
        imEventMessageProducer.produce(imEventMessage);
        return resultModel;
    }

    /**
     * 批量移除群组成员
     *
     * @param creatorId 群主id 必填
     * @param groupId   群组id 必填
     * @param uids      用户id列表
     * @return 执行结果
     */
    @Override
    public ResultModel removeGroupMembers(int creatorId, int groupId, List<Integer> uids) {
        ResultModel resultModel = new ResultModel();
        // 检查群主信息
        ResultModel resultModelCheckGroupOwner = checkGroupOwner(groupId, creatorId);
        if (resultModelCheckGroupOwner.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelCheckGroupOwner);
        }

        // 检查要移除的是否是群主
        if (uids.contains(creatorId)) {
            // 无法移除群主
            return resultModel.setFailed(ImStatusCode.IM_GROUP_OPERATION_FORBIDDEN, "无法移除群主");
        }

        /*
         * 批量移除群组用户
         */
        int removeNumber = 0;
        for (Integer uid : uids) {
            // 查询群组用户信息
            ResultModel<ImGroupMemberModel> resultModelImGroupMember = checkGroupMember(groupId, uid);
            if (resultModelImGroupMember.getCode() != ResultModel.RESULT_SUCCESS) {
                return resultModel.setFailed(resultModelImGroupMember);
            }

            // 删除群组用户
            ImGroupMemberModel imGroupMemberModel = resultModelImGroupMember.getData();
            if (imGroupMemberDAO.deleteImGroupMemberByPK(imGroupMemberModel) > 0) {
                removeNumber++;
            }

            /*
             * 将移除用户消息放入MQ
             */
            ImEventMessage imEventMessage = new ImEventMessage();
            imEventMessage.setGroupId(groupId);
            imEventMessage.setUid(uid);
            imEventMessage.setType(ImEventMessage.REMOVE_MEMBER);
            imEventMessage.setTime(new Date());
            imEventMessageProducer.produce(imEventMessage);
        }

        // 群组用户数-removeNumber
        ImGroupModel imGroupModel = new ImGroupModel(groupId);
        imGroupModel.setCreatorId(creatorId);
        imGroupModel.setMemberCount(-removeNumber);
        imGroupDAO.updateImGroupByPK(imGroupModel);

        return resultModel;
    }

    /**
     * 获取群组信息
     *
     * @param currentUid   当前用户uid 选填
     * @param imGroupModel imGroupModel.id 群组id 必填
     *                     imGroupModel.requireOwner 是否需要群主信息(默认false) 选填
     *                     imGroupModel.requireMember 是否需要群成员信息(默认false) 选填
     * @return 群组信息
     */
    @Override
    public ResultModel<ImGroupModel> group(int currentUid, ImGroupModel imGroupModel) {
        ResultModel<ImGroupModel> resultModel = new ResultModel<>();
        ResultModel<ImGroupMemberModel> resultModelGroupMember = checkGroupMember(imGroupModel.getId(), currentUid);
        if (resultModelGroupMember.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelGroupMember);
        }
        ImGroupModel dbImGroupModel = getImGroup(imGroupModel);
        if (dbImGroupModel == null) {
            return resultModel.setFailed(ImStatusCode.IM_GROUP_NOT_EXISTS, "群组不存在");
        }
        resultModel.setData(dbImGroupModel);
        return resultModel;
    }

    /**
     * 获取群组列表
     *
     * @param uid          用户id
     * @param imGroupModel imGroupModel.pageSize 分页大小(默认20) 选填
     *                     imGroupModel.pageIndex 页码(默认0) 选填
     *                     imGroupModel.requireOwner 是否需要群主信息(默认false) 选填
     * @return 群组列表
     */
    @Override
    public ResultModel<List<ImGroupModel>> groups(int uid, ImGroupModel imGroupModel) {
        ResultModel<List<ImGroupModel>> resultModel = new ResultModel<>();
        ImGroupMemberModel imGroupMemberModelQuery = new ImGroupMemberModel();
        imGroupMemberModelQuery.setPageSize(imGroupModel.getPageSize());
        imGroupMemberModelQuery.setPageIndex(imGroupModel.getPageIndex());
        imGroupMemberModelQuery.setUid(uid);
        // 获取用户所在群组的id列表
        List<Integer> groupIds = imGroupMemberDAO.queryGroupIdListByCriteria(imGroupMemberModelQuery);
        List<ImGroupModel> groupModelList = new ArrayList<>(10);
        ImGroupModel dbImGroupModel;
        for (Integer groupId : groupIds) {
            imGroupModel.setId(groupId);
            // 获取群组信息
            dbImGroupModel = getImGroup(imGroupModel);
            if (dbImGroupModel != null) {
                groupModelList.add(dbImGroupModel);
            }
        }
        resultModel.setData(groupModelList);
        return resultModel;
    }

    /**
     * 加入群组
     *
     * @param uid           用户id 必填
     * @param groupPassword 群组口令 必填
     * @return 群组id
     */
    @Override
    public ResultModel<Integer> joinGroup(int uid, String groupPassword) {
        ResultModel<Integer> resultModel = new ResultModel<>();
        /*
         * 检查群组是否存在
         */
        ImGroupModel imGroupModel = getImGroupByPassword(groupPassword);
        if (imGroupModel == null) {
            // 群组不存在
            return resultModel.setFailed(ImStatusCode.IM_GROUP_NOT_EXISTS, "群组不存在");
        }

        /*
         * 判断用户是否已经加入群组
         */
        ImGroupMemberModel imGroupMemberModel = new ImGroupMemberModel(imGroupModel.getId());
        imGroupMemberModel.setUid(uid);
        Integer id = imGroupMemberDAO.queryPKByCriteria(imGroupMemberModel);
        if (id != null) {
            // 用户已加入群组
            return resultModel.setFailed(ImStatusCode.IM_GROUP_MEMBER_ALREADY_EXISTS, "用户已加入群组");
        }

        /*
         * 检查群成员数是否已经达到最大值
         */
        if (imGroupModel.getMemberCount() >= imGroupModel.getMaxMember()) {
            // 群组人数已达到最大值
            return resultModel.setFailed(ImStatusCode.IM_GROUP_MEMBER_REACH_MAX, "群组人数已达到最大值");
        }

        /*
         * 保存群成员信息
         */
        ResultModel<UserModel> resultModelUser = memberService.getUserById(sid, sid, uid);
        if (resultModelUser.getCode() != ResultModel.RESULT_SUCCESS) {
            // 获取用户信息失败
            return resultModel.setFailed(resultModelUser);
        }
        UserModel um = resultModelUser.getData();
        // 保存群成员信息
        imGroupMemberDAO.insertImGroupMember(new ImGroupMemberModel(um.getId().intValue(), imGroupModel.getId(),
                um.getAccount(), um.getHeadImgUrl(), ImGroupMemberModel.MEMBER));
        // 群组用户数+1
        imGroupModel.setMemberCount(1);
        imGroupDAO.updateImGroupByPK(imGroupModel);
        resultModel.setData(imGroupModel.getId());

        /*
         * 将用户加入群组消息放入MQ
         */
        ImEventMessage imEventMessage = new ImEventMessage();
        imEventMessage.setGroupId(imGroupModel.getId());
        imEventMessage.setUid(um.getId().intValue());
        imEventMessage.setType(ImEventMessage.MEMBER_JOIN);
        imEventMessage.setTime(new Date());
        imEventMessageProducer.produce(imEventMessage);
        return resultModel;
    }

    /**
     * 离开群组
     *
     * @param imGroupMemberModel imGroupMemberModel.uid 用户id 必填
     *                           imGroupMemberModel.groupId 群组id 必填
     * @return 执行结果
     */
    @Override
    public ResultModel leaveGroup(ImGroupMemberModel imGroupMemberModel) {
        ResultModel resultModel = new ResultModel();
        // 查询群组用户信息
        ResultModel<ImGroupMemberModel> resultModelImGroupMember = checkGroupMember(imGroupMemberModel.getGroupId(),
                imGroupMemberModel.getUid());
        if (resultModelImGroupMember.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelImGroupMember);
        }

        // 检查是否是群主
        ImGroupMemberModel dbImGroupMemberModel = resultModelImGroupMember.getData();
        if (dbImGroupMemberModel.getIsOwner() == ImGroupMemberModel.OWNER) {
            // 群主无法离开群组
            return resultModel.setFailed(ImStatusCode.IM_GROUP_OPERATION_FORBIDDEN, "群主无法离开群组");
        }

        imGroupMemberDAO.deleteImGroupMemberByPK(dbImGroupMemberModel);
        // 群组用户数-1
        ImGroupModel imGroupModel = imGroupDAO.queryImGroupByPK(new ImGroupModel(imGroupMemberModel.getGroupId()));
        imGroupModel.setMemberCount(-1);
        imGroupDAO.updateImGroupByPK(imGroupModel);

        /*
         * 将用户离开群组消息放入MQ
         */
        ImEventMessage imEventMessage = new ImEventMessage();
        imEventMessage.setGroupId(imGroupMemberModel.getGroupId());
        imEventMessage.setUid(imGroupMemberModel.getUid());
        imEventMessage.setType(ImEventMessage.MEMBER_LEAVE);
        imEventMessage.setTime(new Date());
        imEventMessageProducer.produce(imEventMessage);
        return resultModel;
    }

    /**
     * 更新群组用户信息
     *
     * @param imGroupMemberModel imGroupMemberModel.uid 用户id 必填
     *                           imGroupMemberModel.groupId 群组id 必填
     *                           imGroupMemberModel.nickname 昵称 选填
     *                           imGroupMemberModel.avatar 头像 选填
     * @return 执行结果
     */
    @Override
    public ResultModel updateGroupMember(ImGroupMemberModel imGroupMemberModel) {
        ResultModel resultModel = new ResultModel();
        // 查询群组用户信息
        ResultModel<ImGroupMemberModel> resultModelImGroupMember = checkGroupMember(imGroupMemberModel.getGroupId(),
                imGroupMemberModel.getUid());
        if (resultModelImGroupMember.getCode() != ResultModel.RESULT_SUCCESS) {
            return resultModel.setFailed(resultModelImGroupMember);
        }
        imGroupMemberModel.setId(resultModelImGroupMember.getData().getId());
        imGroupMemberDAO.updateImGroupMemberByPK(imGroupMemberModel);

        /*
         * 将用户更新消息放入MQ
         */
        ImEventMessage imEventMessage = new ImEventMessage();
        imEventMessage.setGroupId(imGroupMemberModel.getGroupId());
        imEventMessage.setUid(imGroupMemberModel.getUid());
        imEventMessage.setType(ImEventMessage.MEMBER_UPDATE);
        imEventMessage.setTime(new Date());
        imEventMessageProducer.produce(imEventMessage);
        return resultModel;
    }

    /**
     * 获取群组成员信息
     *
     * @param currentUid 当前用户uid 选填
     * @param groupId    群组id
     * @param uid        用户id
     * @return 群组成员信息
     */
    @Override
    public ResultModel<ImGroupMemberModel> groupMember(int currentUid, int groupId, int uid) {
        ResultModel<ImGroupMemberModel> resultModel = new ResultModel<>();
        // 检查当前用户是否在群组
        if (currentUid > 0) {
            ResultModel<ImGroupMemberModel> resultModelCurrentUidCheck = checkGroupMember(groupId, currentUid);
            if (resultModelCurrentUidCheck.getCode() != ResultModel.RESULT_SUCCESS) {
                return resultModel.setFailed(resultModelCurrentUidCheck);
            }
        }

        // 获取群组用户信息
        return checkGroupMember(groupId, uid);
    }

    /**
     * =================================== 私有方法 =================================
     */
    /**
     * 获取可用的口令。
     * 1)生成口令，然后查找数据库中该口令是否已被使用
     * 2)如果被使用，则返回步骤1。否则返回生成的口令
     *
     * @return
     */
    private String getAvailablePassword() {
        ImGroupModel imGroupModel = new ImGroupModel();
        imGroupModel.setPassword(StringUtils.random(6, true));
        while (imGroupDAO.queryPKByPassword(imGroupModel) != null) {
            imGroupModel.setPassword(StringUtils.random(6, true));
        }
        return imGroupModel.getPassword();
    }

    /**
     * 根据口令获取群组信息
     *
     * @param groupPassword 群组口令
     * @return 群组信息
     */
    private ImGroupModel getImGroupByPassword(String groupPassword) {
        ImGroupModel imGroupModelQuery = new ImGroupModel();
        imGroupModelQuery.setPassword(groupPassword);
        Integer id = imGroupDAO.queryPKByPassword(imGroupModelQuery);
        if (id != null) {
            imGroupModelQuery.setId(id);
            return imGroupDAO.queryImGroupByPK(imGroupModelQuery);
        }
        return null;
    }

    /**
     * 获取群组信息
     *
     * @param imGroupModel imGroupModel.id 群组id 必填
     *                     imGroupModel.requireOwner 是否需要群主信息(默认false) 选填
     *                     imGroupModel.requireMember 是否需要群成员信息(默认false) 选填
     * @return 群组信息
     */
    private ImGroupModel getImGroup(ImGroupModel imGroupModel) {
        // 获取群组信息
        ImGroupModel dbImGroupModel = imGroupDAO.queryImGroupByPK(imGroupModel);

        // 群组不存在
        if (dbImGroupModel == null) {
            return null;
        }

        /*
         * 需要获取群主信息
         */
        if (imGroupModel.isRequireOwner()) {
            ImGroupMemberModel imGroupMemberModelQuery = new ImGroupMemberModel(imGroupModel.getId());
            imGroupMemberModelQuery.setUid(dbImGroupModel.getCreatorId());
            imGroupMemberModelQuery.setId(imGroupMemberDAO.queryPKByCriteria(imGroupMemberModelQuery));
            dbImGroupModel.setOwner(imGroupMemberDAO.queryImGroupMemberByPK(imGroupMemberModelQuery));
        }

        /*
         * 需要获取群组成员信息
         */
        if (imGroupModel.isRequireMember()) {
            ImGroupMemberModel imGroupMemberModelQuery = new ImGroupMemberModel(imGroupModel.getId());
            List<Integer> groupMemberIds = imGroupMemberDAO.queryPKListByCriteria(imGroupMemberModelQuery);
            List<ImGroupMemberModel> groupMembers = new ArrayList<>(10);
            for (Integer id : groupMemberIds) {
                imGroupMemberModelQuery.setId(id);
                groupMembers.add(imGroupMemberDAO.queryImGroupMemberByPK(imGroupMemberModelQuery));
            }
            dbImGroupModel.setMembers(groupMembers);
        }
        return dbImGroupModel;
    }

    /**
     * 检查用户是否在群组
     *
     * @param groupId 群组id
     * @param uid     用户id
     * @return 检查用户是否在群组
     */
    private ResultModel<ImGroupMemberModel> checkGroupMember(int groupId, int uid) {
        ResultModel<ImGroupMemberModel> resultModel = new ResultModel<>();

        /*
         * 检查群组是否存在
         */
        ImGroupModel imGroupModel = imGroupDAO.queryImGroupByPK(new ImGroupModel(groupId));
        if (imGroupModel == null) {
            // 群组不存在
            return resultModel.setFailed(ImStatusCode.IM_GROUP_NOT_EXISTS, "群组不存在");
        }

        /*
         * 获取群组中的用户
         */
        ImGroupMemberModel imGroupMemberModel = new ImGroupMemberModel(groupId);
        imGroupMemberModel.setUid(uid);
        Integer id = imGroupMemberDAO.queryPKByCriteria(imGroupMemberModel);
        if (id == null) {
            // 用户不在群组
            return resultModel.setFailed(ImStatusCode.IM_GROUP_MEMBER_NOT_EXISTS, "用户不在群组");
        }
        imGroupMemberModel.setId(id);
        resultModel.setData(imGroupMemberDAO.queryImGroupMemberByPK(imGroupMemberModel));
        return resultModel;
    }

    /**
     * 检查是否是群主
     *
     * @param groupId   群组id
     * @param creatorId 群主id
     * @return 检查是否是群主
     */
    private ResultModel checkGroupOwner(int groupId, int creatorId) {
        ResultModel resultModel = new ResultModel();
        /*
         * 检查群组是否存在
         */
        ImGroupModel imGroupModel = imGroupDAO.queryImGroupByPK(new ImGroupModel(groupId));
        if (imGroupModel == null) {
            // 群组不存在
            return resultModel.setFailed(ImStatusCode.IM_GROUP_NOT_EXISTS, "群组不存在");
        }

        /*
         * 检查是否是群主
         */
        if (imGroupModel.getCreatorId() != creatorId) {
            // 当前用户无权限
            return resultModel.setFailed(ImStatusCode.IM_GROUP_OPERATION_FORBIDDEN, "当前用户无权限");
        }
        return resultModel;
    }

}