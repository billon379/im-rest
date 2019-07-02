package fun.billon.im.rest.dao;

import fun.billon.common.cache.CacheType;
import fun.billon.common.cache.annotation.CacheEvict;
import fun.billon.common.cache.annotation.Cacheable;
import fun.billon.im.api.constant.ImCacheConstant;
import fun.billon.im.api.model.ImGroupModel;
import org.springframework.stereotype.Repository;

/**
 * im群组DAO
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface IImGroupDAO {

    /**
     * 新增群组
     *
     * @param imGroupModel imGroupModel.name 群组名称 必填
     *                     imGroupModel.password 口令 必填
     *                     imGroupModel.passwordExpireTime 口令过期时间 必填
     *                     imGroupModel.creatorId 群主id 必填
     *                     imGroupModel.destination 目的地 选填
     *                     imGroupModel.latitude 目的地纬度(gps) 选填
     *                     imGroupModel.longitude 目的地经度(gps) 选填
     *                     imGroupModel.maxMember 最大成员数量(默认10) 选填
     * @return 操作结果(数据库受影响的行数)
     */
    int insertImGroup(ImGroupModel imGroupModel);

    /**
     * 根据主键删除群组
     *
     * @param imGroupModel imGroupModel.id 主键 必填
     *                     imGroupModel.creatorId 群主id 必填
     * @return 操作结果(数据库受影响的行数)
     */
    @CacheEvict(namespace = ImCacheConstant.CACHE_NAMESPACE_IM_GROUP_MODEL,
            key = ImCacheConstant.CACHE_KEY_IM_GROUP_MODEL_ID)
    int deleteImGroupByPK(ImGroupModel imGroupModel);

    /**
     * 更新群组
     *
     * @param imGroupModel imGroupModel.id 主键 必填
     *                     imGroupModel.creatorId 群主id 必填
     *                     imGroupModel.name 群组名称 选填
     *                     imGroupModel.destination 目的地 选填
     *                     imGroupModel.latitude 目的地纬度(gps) 选填
     *                     imGroupModel.longitude 目的地经度(gps) 选填
     *                     imGroupModel.password 口令 选填
     *                     imGroupModel.passwordExpireTime 口令过期时间 选填
     *                     imGroupModel.memberCount 群成员数 选填
     * @return 操作结果(数据库受影响的行数)
     */
    @CacheEvict(namespace = ImCacheConstant.CACHE_NAMESPACE_IM_GROUP_MODEL,
            key = ImCacheConstant.CACHE_KEY_IM_GROUP_MODEL_ID)
    int updateImGroupByPK(ImGroupModel imGroupModel);

    /**
     * 根据主键获取群组信息
     *
     * @param imGroupModel imGroupModel.id 主键 必填
     * @return 群组信息
     */
    @Cacheable(namespace = ImCacheConstant.CACHE_NAMESPACE_IM_GROUP_MODEL,
            key = ImCacheConstant.CACHE_KEY_IM_GROUP_MODEL_ID,
            type = ImGroupModel.class, cacheType = CacheType.HASH)
    ImGroupModel queryImGroupByPK(ImGroupModel imGroupModel);

    /**
     * 根据口令查询群组id
     *
     * @param imGroupModel imGroupModel.password 口令 必填
     * @return 群组id
     */
    Integer queryPKByPassword(ImGroupModel imGroupModel);

}