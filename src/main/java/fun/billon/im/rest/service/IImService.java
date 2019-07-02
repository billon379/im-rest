package fun.billon.im.rest.service;

import fun.billon.common.model.ResultModel;
import fun.billon.im.api.model.ImGroupMemberModel;
import fun.billon.im.api.model.ImGroupModel;

import java.util.List;

/**
 * im服务层接口
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
public interface IImService {

    /**
     * 创建群组
     *
     * @param imGroupModel imGroupModel.name 群组名称 必填
     *                     imGroupModel.creatorId 群主id 必填
     *                     imGroupModel.destination 目的地 选填
     *                     imGroupModel.latitude 目的地纬度(gps) 选填
     *                     imGroupModel.longitude 目的地经度(gps) 选填
     * @param nickname     群主昵称 选填
     * @return 群组id
     */
    ResultModel<Integer> createGroup(ImGroupModel imGroupModel, String nickname);

    /**
     * 解散群组
     *
     * @param imGroupModel imGroupModel.id 主键 必填
     *                     imGroupModel.creatorId 群主id 必填
     * @return 执行结果
     */
    ResultModel deleteGroup(ImGroupModel imGroupModel);

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
    ResultModel updateGroup(ImGroupModel imGroupModel, String nickname);

    /**
     * 更新群组口令
     *
     * @param imGroupModel imGroupModel.id 主键 必填
     *                     imGroupModel.creatorId 群主id 必填
     * @return 群组信息
     */
    ResultModel<ImGroupModel> updateGroupPassword(ImGroupModel imGroupModel);

    /**
     * 移除群组成员
     *
     * @param creatorId 群主id 必填
     * @param groupId   群组id 必填
     * @param uid       用户uid 必填
     * @return 执行结果
     */
    ResultModel removeGroupMember(int creatorId, int groupId, int uid);

    /**
     * 批量移除群组成员
     *
     * @param creatorId 群主id 必填
     * @param groupId   群组id 必填
     * @param uids      用户id列表
     * @return 执行结果
     */
    ResultModel removeGroupMembers(int creatorId, int groupId, List<Integer> uids);

    /**
     * 获取群组信息
     *
     * @param currentUid   当前用户uid 选填
     * @param imGroupModel imGroupModel.id 群组id 必填
     *                     imGroupModel.requireOwner 是否需要群主信息(默认false) 选填
     *                     imGroupModel.requireMember 是否需要群成员信息(默认false) 选填
     * @return 群组信息
     */
    ResultModel<ImGroupModel> group(int currentUid, ImGroupModel imGroupModel);

    /**
     * 获取群组列表
     *
     * @param uid          用户id
     * @param imGroupModel imGroupModel.pageSize 分页大小(默认20) 选填
     *                     imGroupModel.pageIndex 页码(默认0) 选填
     *                     imGroupModel.requireOwner 是否需要群主信息(默认false) 选填
     * @return 群组列表
     */
    ResultModel<List<ImGroupModel>> groups(int uid, ImGroupModel imGroupModel);

    /**
     * 加入群组
     *
     * @param uid           用户id 必填
     * @param groupPassword 群组口令 必填
     * @return 群组id
     */
    ResultModel<Integer> joinGroup(int uid, String groupPassword);

    /**
     * 离开群组
     *
     * @param imGroupMemberModel imGroupMemberModel.uid 用户id 必填
     *                           imGroupMemberModel.groupId 群组id 必填
     * @return 执行结果
     */
    ResultModel leaveGroup(ImGroupMemberModel imGroupMemberModel);

    /**
     * 更新群组用户信息
     *
     * @param imGroupMemberModel imGroupMemberModel.uid 用户id 必填
     *                           imGroupMemberModel.groupId 群组id 必填
     *                           imGroupMemberModel.nickname 昵称 选填
     *                           imGroupMemberModel.avatar 头像 选填
     * @return 执行结果
     */
    ResultModel updateGroupMember(ImGroupMemberModel imGroupMemberModel);

    /**
     * 获取群组成员信息
     *
     * @param currentUid 当前用户uid 选填
     * @param groupId    群组id 必填
     * @param uid        用户id 必填
     * @return 群组成员信息
     */
    ResultModel<ImGroupMemberModel> groupMember(int currentUid, int groupId, int uid);

}