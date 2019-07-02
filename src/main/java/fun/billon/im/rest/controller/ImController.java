package fun.billon.im.rest.controller;

import fun.billon.common.constant.CommonStatusCode;
import fun.billon.common.exception.ParamException;
import fun.billon.common.model.ResultModel;
import fun.billon.common.util.StringUtils;
import fun.billon.im.api.model.ImGroupMemberModel;
import fun.billon.im.api.model.ImGroupModel;
import fun.billon.im.rest.service.IImService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * im接口
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
public class ImController {

    @Resource
    private IImService imService;

    /**
     * 创建群组
     *
     * @param paramMap paramMap.name 群组名称 必填
     *                 paramMap.creatorId 群主id 必填
     *                 paramMap.destination 目的地 选填
     *                 paramMap.latitude 目的地纬度(gps) 选填
     *                 paramMap.longitude 目的地经度(gps) 选填
     *                 paramMap.nickname 群主昵称 选填
     * @return 操作结果
     */
    @PostMapping("/group")
    public ResultModel<Integer> createGroup(@RequestParam Map<String, String> paramMap) {
        ResultModel<Integer> resultModel = new ResultModel<>();
        String[] paramArray = new String[]{"name", "creatorId", "destination", "latitude", "longitude", "nickname"};
        boolean[] requiredArray = new boolean[]{true, true, false, false, false, false};
        Class[] classArray = new Class[]{String.class, Integer.class, String.class, Double.class, Double.class, String.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }

        ImGroupModel imGroupModel = new ImGroupModel();
        imGroupModel.setName(paramMap.get("name"));
        imGroupModel.setDestination(paramMap.get("destination"));
        if (!StringUtils.isEmpty(paramMap.get("latitude"))) {
            imGroupModel.setLatitude(Double.valueOf(paramMap.get("latitude")));
        }
        if (!StringUtils.isEmpty(paramMap.get("longitude"))) {
            imGroupModel.setLongitude(Double.valueOf(paramMap.get("longitude")));
        }
        imGroupModel.setCreatorId(Integer.valueOf(paramMap.get("creatorId")));
        return imService.createGroup(imGroupModel, paramMap.get("nickname"));
    }

    /**
     * 解散群组
     *
     * @param groupId  群组id 必填
     * @param paramMap paramMap.creatorId 群主id 必填
     * @return 操作结果
     */
    @DeleteMapping("/group/{groupId}")
    public ResultModel deleteGroup(@PathVariable(value = "groupId") int groupId,
                                   @RequestParam Map<String, String> paramMap) {
        ResultModel resultModel = new ResultModel();
        String[] paramArray = new String[]{"creatorId"};
        boolean[] requiredArray = new boolean[]{true};
        Class[] classArray = new Class[]{Integer.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }

        ImGroupModel imGroupModel = new ImGroupModel(groupId);
        imGroupModel.setName(paramMap.get("name"));
        imGroupModel.setCreatorId(Integer.valueOf(paramMap.get("creatorId")));
        return imService.deleteGroup(imGroupModel);
    }

    /**
     * 更新群组
     *
     * @param groupId  群组id 必填
     * @param paramMap paramMap.creatorId 群主id 必填
     *                 paramMap.name 群组名称 选填
     *                 paramMap.destination 目的地 选填
     *                 paramMap.latitude 目的地纬度(gps) 选填
     *                 paramMap.longitude 目的地经度(gps) 选填
     *                 paramMap.nickname 群主昵称 选填
     * @return 操作结果
     */
    @PutMapping("/group/{groupId}")
    public ResultModel updateGroup(@PathVariable(value = "groupId") int groupId,
                                   @RequestParam Map<String, String> paramMap) {
        ResultModel resultModel = new ResultModel();
        String[] paramArray = new String[]{"creatorId", "name", "destination", "latitude", "longitude", "nickname"};
        boolean[] requiredArray = new boolean[]{true, false, false, false, false, false};
        Class[] classArray = new Class[]{Integer.class, String.class, String.class, Double.class, Double.class, String.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }

        ImGroupModel imGroupModel = new ImGroupModel(groupId);
        imGroupModel.setCreatorId(Integer.valueOf(paramMap.get("creatorId")));
        imGroupModel.setName(paramMap.get("name"));
        imGroupModel.setDestination(paramMap.get("destination"));
        if (!StringUtils.isEmpty(paramMap.get("latitude"))) {
            imGroupModel.setLatitude(Double.valueOf(paramMap.get("latitude")));
        }
        if (!StringUtils.isEmpty(paramMap.get("longitude"))) {
            imGroupModel.setLongitude(Double.valueOf(paramMap.get("longitude")));
        }
        return imService.updateGroup(imGroupModel, paramMap.get("nickname"));
    }

    /**
     * 更新群组口令
     *
     * @param groupId  群组id 必填
     * @param paramMap paramMap.creatorId 群主id 必填
     * @return 群组信息
     */
    @PutMapping("/group/{groupId}/password")
    public ResultModel<ImGroupModel> updateGroupPassword(@PathVariable(value = "groupId") int groupId,
                                                         @RequestParam Map<String, String> paramMap) {
        ResultModel resultModel = new ResultModel();
        String[] paramArray = new String[]{"creatorId"};
        boolean[] requiredArray = new boolean[]{true};
        Class[] classArray = new Class[]{Integer.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }

        ImGroupModel imGroupModel = new ImGroupModel(groupId);
        imGroupModel.setCreatorId(Integer.valueOf(paramMap.get("creatorId")));
        return imService.updateGroupPassword(imGroupModel);
    }

    /**
     * 移除群组成员
     *
     * @param groupId  群组id 必填
     * @param uid      用户id 必填
     * @param paramMap paramMap.creatorId 群主id 必填
     * @return 操作结果
     */
    @DeleteMapping("/group/{groupId}/member/{uid}")
    public ResultModel removeGroupMember(@PathVariable(value = "groupId") int groupId,
                                         @PathVariable(value = "uid") int uid,
                                         @RequestParam Map<String, String> paramMap) {
        ResultModel resultModel = new ResultModel();
        String[] paramArray = new String[]{"creatorId"};
        boolean[] requiredArray = new boolean[]{true};
        Class[] classArray = new Class[]{Integer.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }
        int creatorId = Integer.valueOf(paramMap.get("creatorId"));
        return imService.removeGroupMember(creatorId, groupId, uid);
    }

    /**
     * 批量移除群组成员
     *
     * @param groupId  群组id 必填
     * @param uids     用户id列表(多个用户使用","分隔) 必填
     * @param paramMap paramMap.creatorId 群主id 必填
     * @return 操作结果
     */
    @DeleteMapping("/group/{groupId}/member/uids/{uids}")
    public ResultModel removeGroupMembers(@PathVariable(value = "groupId") int groupId,
                                          @PathVariable(value = "uids") String uids,
                                          @RequestParam Map<String, String> paramMap) {
        ResultModel resultModel = new ResultModel();
        String[] paramArray = new String[]{"creatorId"};
        boolean[] requiredArray = new boolean[]{true};
        Class[] classArray = new Class[]{Integer.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }
        int creatorId = Integer.valueOf(paramMap.get("creatorId"));
        String[] uidArray = uids.split(",");
        List<Integer> uidList = new ArrayList<>();
        for (String uid : uidArray) {
            try {
                uidList.add(Integer.valueOf(uid));
            } catch (NumberFormatException e) {
                return resultModel.setFailed(CommonStatusCode.PARAM_INVALID, "参数[" + uid + "]类型不合法");
            }
        }
        return imService.removeGroupMembers(creatorId, groupId, uidList);
    }

    /**
     * 获取群组信息
     *
     * @param groupId  群组id 必填
     * @param paramMap paramMap.currentUid 当前用户uid 选填
     *                 paramMap.requireOwner 是否需要群主信息(默认false) 选填
     *                 paramMap.requireMember 是否需要群成员信息(默认false) 选填
     * @return 操作结果
     */
    @GetMapping("/group/{groupId}")
    public ResultModel<ImGroupModel> group(@PathVariable(value = "groupId") int groupId,
                                           @RequestParam Map<String, String> paramMap) {
        ResultModel<ImGroupModel> resultModel = new ResultModel<>();
        String[] paramArray = new String[]{"currentUid", "requireOwner", "requireMember"};
        boolean[] requiredArray = new boolean[]{false, false, false};
        Class[] classArray = new Class[]{Integer.class, Integer.class, Integer.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }

        int currentUid = 0;
        if (!StringUtils.isEmpty(paramMap.get("currentUid"))) {
            currentUid = Integer.parseInt(paramMap.get("currentUid"));
        }

        ImGroupModel imGroupModel = new ImGroupModel(groupId);
        String requireOwner = paramMap.get("requireOwner");
        if (!StringUtils.isEmpty(requireOwner)) {
            if (Integer.parseInt(requireOwner) == 1) {
                imGroupModel.setRequireOwner(true);
            }
        }
        String requireMember = paramMap.get("requireMember");
        if (!StringUtils.isEmpty(requireMember)) {
            if (Integer.parseInt(requireMember) == 1) {
                imGroupModel.setRequireMember(true);
            }
        }
        return imService.group(currentUid, imGroupModel);
    }

    /**
     * 获取群组列表
     *
     * @param uid      用户id 必填
     * @param paramMap paramMap.pageSize 分页大小(默认20) 选填
     *                 paramMap.pageIndex 页码(默认0) 选填
     *                 paramMap.requireOwner 是否需要群主信息(默认false) 选填
     * @return 操作结果
     */
    @GetMapping("/group/uid/{uid}")
    public ResultModel<List<ImGroupModel>> groups(@PathVariable(value = "uid") int uid,
                                                  @RequestParam Map<String, String> paramMap) {
        ResultModel<List<ImGroupModel>> resultModel = new ResultModel<>();
        String[] paramArray = new String[]{"pageSize", "pageIndex", "requireOwner"};
        boolean[] requiredArray = new boolean[]{false, false, false};
        Class[] classArray = new Class[]{Integer.class, Integer.class, Integer.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }

        ImGroupModel imGroupModel = new ImGroupModel();
        if (!StringUtils.isEmpty(paramMap.get("pageSize"))) {
            imGroupModel.setPageSize(Integer.valueOf(paramMap.get("pageSize")));
        }
        if (!StringUtils.isEmpty(paramMap.get("pageIndex"))) {
            imGroupModel.setPageIndex(Integer.valueOf(paramMap.get("pageIndex")));
        }
        String requireOwner = paramMap.get("requireOwner");
        if (!StringUtils.isEmpty(requireOwner)) {
            if (Integer.parseInt(requireOwner) == 1) {
                imGroupModel.setRequireOwner(true);
            }
        }
        return imService.groups(uid, imGroupModel);
    }

    /**
     * 加入群组
     *
     * @param paramMap paramMap.uid      用户id 必填
     *                 paramMap.groupPassword 群组口令 必填
     * @return 操作结果
     */
    @PostMapping("/group/member")
    public ResultModel joinGroup(@RequestParam Map<String, String> paramMap) {
        ResultModel resultModel = new ResultModel();
        String[] paramArray = new String[]{"uid", "groupPassword"};
        boolean[] requiredArray = new boolean[]{true, true};
        Class[] classArray = new Class[]{Integer.class, String.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }

        int uid = Integer.valueOf(paramMap.get("uid"));
        String groupPassword = paramMap.get("groupPassword");
        return imService.joinGroup(uid, groupPassword);
    }

    /**
     * 离开群组
     *
     * @param groupId 群组id 必填
     * @param uid     用户id 必填
     * @return 操作结果
     */
    @DeleteMapping("/group/{groupId}/{uid}")
    public ResultModel leaveGroup(@PathVariable(value = "groupId") int groupId,
                                  @PathVariable(value = "uid") int uid) {
        ImGroupMemberModel imGroupMemberModel = new ImGroupMemberModel(groupId);
        imGroupMemberModel.setUid(uid);
        return imService.leaveGroup(imGroupMemberModel);
    }

    /**
     * 更新群组用户信息
     *
     * @param groupId  群组id 必填
     * @param uid      用户id 必填
     * @param paramMap paramMap.nickname 昵称 选填
     *                 paramMap.avatar 头像 选填
     * @return 操作结果
     */
    @PutMapping("/group/{groupId}/{uid}")
    public ResultModel updateGroupMember(@PathVariable(value = "groupId") int groupId,
                                         @PathVariable(value = "uid") int uid,
                                         @RequestParam Map<String, String> paramMap) {
        ResultModel resultModel = new ResultModel();
        String[] paramArray = new String[]{"nickname", "avatar"};
        boolean[] requiredArray = new boolean[]{false, false};
        Class[] classArray = new Class[]{String.class, String.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }
        ImGroupMemberModel imGroupMemberModel = new ImGroupMemberModel(groupId);
        imGroupMemberModel.setUid(uid);
        imGroupMemberModel.setNickname(paramMap.get("nickname"));
        imGroupMemberModel.setAvatar(paramMap.get("avatar"));
        return imService.updateGroupMember(imGroupMemberModel);
    }

    /**
     * 获取群组用户信息
     *
     * @param groupId  群组id 必填
     * @param uid      用户id 必填
     * @param paramMap paramMap.currentUid 当前用户uid 选填
     * @return 群组用户信息
     */
    @GetMapping("/group/{groupId}/{uid}")
    public ResultModel<ImGroupMemberModel> groupMember(@PathVariable(value = "groupId") int groupId,
                                                       @PathVariable(value = "uid") int uid,
                                                       @RequestParam Map<String, String> paramMap) {
        ResultModel<ImGroupMemberModel> resultModel = new ResultModel<>();
        String[] paramArray = new String[]{"currentUid"};
        boolean[] requiredArray = new boolean[]{false};
        Class[] classArray = new Class[]{Integer.class};
        try {
            StringUtils.checkParam(paramMap, paramArray, requiredArray, classArray, null);
        } catch (ParamException e) {
            resultModel.setFailed(CommonStatusCode.PARAM_INVALID, e.getMessage());
            return resultModel;
        }

        int currentUid = 0;
        if (!StringUtils.isEmpty(paramMap.get("currentUid"))) {
            currentUid = Integer.parseInt(paramMap.get("currentUid"));
        }
        return imService.groupMember(currentUid, groupId, uid);
    }

}