package fun.billon.im.rest.dao;

import fun.billon.common.cache.CacheType;
import fun.billon.common.cache.annotation.CacheEvict;
import fun.billon.common.cache.annotation.Cacheable;
import fun.billon.im.api.constant.ImCacheConstant;
import fun.billon.im.api.model.ImGroupMemberModel;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * im群组成员DAO
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface IImGroupMemberDAO {

    /**
     * 插入群组成员
     *
     * @param imGroupMemberModel imGroupMemberModel.uid 用户id 必填
     *                           imGroupMemberModel.groupId 群组id 必填
     *                           imGroupMemberModel.nickname 昵称 必填
     *                           imGroupMemberModel.avatar 头像 必填
     *                           imGroupMemberModel.isOwner 是否群主(0:成员;1:群主) 必填
     * @return 操作结果(数据库受影响的行数)
     */
    int insertImGroupMember(ImGroupMemberModel imGroupMemberModel);

    /**
     * 根据主键删除群组成员
     *
     * @param imGroupMemberModel imGroupMemberModel.id 主键 必填
     *                           imGroupMemberModel.groupId 群组id 必填
     *                           imGroupMemberModel.uid 用户id 选填
     * @return 操作结果(数据库受影响的行数)
     */
    @CacheEvict(namespace = ImCacheConstant.CACHE_NAMESPACE_IM_GROUP_MEMBER_MODEL,
            key = ImCacheConstant.CACHE_KEY_IM_GROUP_MEMBER_MODEL_ID)
    int deleteImGroupMemberByPK(ImGroupMemberModel imGroupMemberModel);

    /**
     * 根据主键更新群组用户信息
     *
     * @param imGroupMemberModel imGroupMemberModel.id 主键 必填
     *                           imGroupMemberModel.groupId 群组id 必填
     *                           imGroupMemberModel.uid 用户id 选填
     *                           imGroupMemberModel.nickname 昵称 选填
     *                           imGroupMemberModel.avatar 头像 选填
     * @return 操作结果(数据库受影响的行数)
     */
    @CacheEvict(namespace = ImCacheConstant.CACHE_NAMESPACE_IM_GROUP_MEMBER_MODEL,
            key = ImCacheConstant.CACHE_KEY_IM_GROUP_MEMBER_MODEL_ID)
    int updateImGroupMemberByPK(ImGroupMemberModel imGroupMemberModel);

    /**
     * 根据条件获取群组用户id
     *
     * @param imGroupMemberModel imGroupMemberModel.uid 用户id 必填
     *                           imGroupMemberModel.groupId 群组id 必填
     * @return 群组用户id列表
     */
    Integer queryPKByCriteria(ImGroupMemberModel imGroupMemberModel);

    /**
     * 根据条件获取群组用户id列表
     *
     * @param imGroupMemberModel imGroupMemberModel.groupId 群组id 选填
     * @return 群组用户id列表
     */
    List<Integer> queryPKListByCriteria(ImGroupMemberModel imGroupMemberModel);

    /**
     * 根据主键获取群组成员信息
     *
     * @param imGroupMemberModel imGroupMemberModel.id 群组成员id 必填
     * @return 群组成员信息
     */
    @Cacheable(namespace = ImCacheConstant.CACHE_NAMESPACE_IM_GROUP_MEMBER_MODEL,
            key = ImCacheConstant.CACHE_KEY_IM_GROUP_MEMBER_MODEL_ID,
            type = ImGroupMemberModel.class, cacheType = CacheType.HASH)
    ImGroupMemberModel queryImGroupMemberByPK(ImGroupMemberModel imGroupMemberModel);

    /**
     * 根据条件获取群组id列表
     *
     * @param imGroupMemberModel imGroupMemberModel.uid 用户id 必填
     * @return 群组id列表
     */
    List<Integer> queryGroupIdListByCriteria(ImGroupMemberModel imGroupMemberModel);

}