package com.cheng.meetlink.controller;

import cn.hutool.json.JSONObject;
import com.cheng.meetlink.annotation.UrlLimit;
import com.cheng.meetlink.annotation.Userid;
import com.cheng.meetlink.service.RoomService;
import com.cheng.meetlink.utils.ResultUtil;
import com.cheng.meetlink.vo.room.CreateRoomVo;
import com.cheng.meetlink.vo.room.JoinRoomVo;
import com.cheng.meetlink.vo.room.LeaveRoomVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/room")
public class RoomController {

    @Resource
    RoomService roomService;

    /**
     * 获取当前用户已加入的聊天室列表
     */
    @UrlLimit
    @GetMapping("/list")
    public Object list(@Userid String userId) {
        List<JSONObject> result = roomService.listUserRooms(userId);
        return ResultUtil.Succeed(result);
    }

    /**
     * 加入聊天室
     */
    @UrlLimit
    @PostMapping("/join")
    public Object join(@Userid String userId, @RequestBody @Valid JoinRoomVo vo) {
        JSONObject result = roomService.joinRoom(userId, vo.getRoomId(), vo.getPassword());
        return ResultUtil.Succeed(result);
    }

    /**
     * 新建聊天室
     */
    @UrlLimit
    @PostMapping("/create")
    public Object create(@Userid String userId, @RequestBody @Valid CreateRoomVo vo) {
        JSONObject result = roomService.createRoom(userId, vo.getRoomId(), vo.getPassword(),
                vo.getName(), vo.getInviteCode(), vo.getDescription());
        return ResultUtil.Succeed(result);
    }

    /**
     * 退出聊天室
     */
    @UrlLimit
    @PostMapping("/leave")
    public Object leave(@Userid String userId, @RequestBody @Valid LeaveRoomVo vo) {
        roomService.leaveRoom(userId, vo.getRoomId());
        return ResultUtil.Succeed(null);
    }
}
