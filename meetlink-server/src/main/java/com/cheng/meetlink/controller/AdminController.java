package com.cheng.meetlink.controller;

import cn.hutool.json.JSONObject;
import com.cheng.meetlink.annotation.UrlLimit;
import com.cheng.meetlink.annotation.Userid;
import com.cheng.meetlink.entity.InviteCode;
import com.cheng.meetlink.entity.User;
import com.cheng.meetlink.exception.MeetLinkException;
import com.cheng.meetlink.service.InviteCodeService;
import com.cheng.meetlink.service.RoomService;
import com.cheng.meetlink.service.UserService;
import com.cheng.meetlink.utils.ResultUtil;
import com.cheng.meetlink.vo.admin.DisbandRoomVo;
import com.cheng.meetlink.vo.admin.InviteCodeVo;
import com.cheng.meetlink.vo.room.UpdateRoomVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Resource
    UserService userService;

    @Resource
    InviteCodeService inviteCodeService;

    @Resource
    RoomService roomService;

    /**
     * 校验是否为管理员
     */
    private void checkAdmin(String userId) {
        User user = userService.getById(userId);
        if (user == null || !"admin".equals(user.getRole())) {
            throw new MeetLinkException("无权限~");
        }
    }

    /**
     * 获取所有聊天室（管理员）
     */
    @UrlLimit
    @GetMapping("/rooms")
    public Object listRooms(@Userid String userId) {
        checkAdmin(userId);
        List<JSONObject> result = roomService.listAllRooms();
        return ResultUtil.Succeed(result);
    }

    /**
     * 解散聊天室（管理员）
     */
    @UrlLimit
    @PostMapping("/room/disband")
    public Object disbandRoom(@Userid String userId, @RequestBody @Valid DisbandRoomVo vo) {
        checkAdmin(userId);
        roomService.disbandRoom(vo.getRoomId());
        return ResultUtil.Succeed(null);
    }

    /**
     * 修改聊天室信息（管理员）
     */
    @UrlLimit
    @PostMapping("/room/update")
    public Object updateRoom(@Userid String userId, @RequestBody @Valid UpdateRoomVo vo) {
        checkAdmin(userId);
        roomService.updateRoom(vo.getRoomId(), vo.getName(), vo.getDescription(),
                vo.getPassword(), vo.getMaxMembers());
        return ResultUtil.Succeed(null);
    }

    /**
     * 生成邀请码（管理员）
     */
    @UrlLimit
    @PostMapping("/invite-code")
    public Object generateInviteCode(@Userid String userId, @RequestBody InviteCodeVo vo) {
        checkAdmin(userId);
        InviteCode result = inviteCodeService.generate(userId, vo.getMaxMembers());
        return ResultUtil.Succeed(result);
    }

    /**
     * 邀请码列表（管理员）
     */
    @UrlLimit
    @GetMapping("/invite-code/list")
    public Object listInviteCodes(@Userid String userId) {
        checkAdmin(userId);
        List<InviteCode> result = inviteCodeService.listAll();
        return ResultUtil.Succeed(result);
    }

    /**
     * 废弃邀请码（管理员）
     */
    @UrlLimit
    @DeleteMapping("/invite-code/{id}")
    public Object invalidateInviteCode(@Userid String userId, @PathVariable String id) {
        checkAdmin(userId);
        inviteCodeService.invalidate(id);
        return ResultUtil.Succeed(null);
    }
}
