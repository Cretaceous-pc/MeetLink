package com.cheng.meetlink.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cheng.meetlink.entity.ChatList;
import com.cheng.meetlink.entity.Group;
import com.cheng.meetlink.entity.InviteCode;
import com.cheng.meetlink.entity.RoomMember;
import com.cheng.meetlink.exception.MeetLinkException;
import com.cheng.meetlink.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    @Resource
    GroupService groupService;

    @Resource
    RoomMemberService roomMemberService;

    @Resource
    InviteCodeService inviteCodeService;

    @Resource
    ChatListService chatListService;

    @Resource
    WebSocketService webSocketService;

    @Override
    public List<JSONObject> listUserRooms(String userId) {
        LambdaQueryWrapper<RoomMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomMember::getUserId, userId);
        List<RoomMember> members = roomMemberService.list(wrapper);

        List<JSONObject> result = new ArrayList<>();
        for (RoomMember member : members) {
            Group group = groupService.getById(member.getGroupId());
            if (group != null) {
                JSONObject room = new JSONObject();
                room.put("roomId", group.getId());
                room.put("name", group.getName());
                room.put("avatar", group.getAvatar());
                room.put("description", group.getDescription());
                room.put("maxMembers", group.getMaxMembers());
                // 统计在线人数（从 WebSocket 房间映射中取，如果没有则返回 0）
                int onlineCount = webSocketService.getRoomOnlineCount(group.getId());
                room.put("onlineCount", onlineCount);
                result.add(room);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public JSONObject joinRoom(String userId, String roomId, String password) {
        Group group = groupService.getById(roomId);
        if (group == null) {
            throw new MeetLinkException("聊天室不存在~");
        }
        if (!group.getPassword().equals(password)) {
            throw new MeetLinkException("聊天室密码错误~");
        }
        // 检查是否已是成员
        LambdaQueryWrapper<RoomMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomMember::getUserId, userId)
               .eq(RoomMember::getGroupId, roomId);
        if (roomMemberService.count(wrapper) > 0) {
            throw new MeetLinkException("你已在此聊天室中~");
        }
        // 检查人数上限
        long memberCount = roomMemberService.count(
                new LambdaQueryWrapper<RoomMember>().eq(RoomMember::getGroupId, roomId));
        if (memberCount >= group.getMaxMembers()) {
            throw new MeetLinkException("聊天室人数已满~");
        }
        // 加入
        RoomMember member = new RoomMember();
        member.setId(IdUtil.simpleUUID());
        member.setUserId(userId);
        member.setGroupId(roomId);
        member.setRole("member");
        member.setJoinTime(new Date());
        roomMemberService.save(member);

        JSONObject result = new JSONObject();
        result.put("roomId", group.getId());
        result.put("name", group.getName());
        result.put("avatar", group.getAvatar());
        result.put("description", group.getDescription());
        result.put("maxMembers", group.getMaxMembers());
        return result;
    }

    @Override
    @Transactional
    public JSONObject createRoom(String userId, String roomId, String password, String name,
                                  String inviteCode, String description) {
        // 验证聊天室编号唯一
        if (groupService.getById(roomId) != null) {
            throw new MeetLinkException("聊天室编号已存在~");
        }
        // 验证邀请码
        InviteCode usedCode = inviteCodeService.validateAndUse(inviteCode, userId, roomId);
        // 创建聊天室
        Group group = new Group();
        group.setId(roomId);
        group.setName(name);
        group.setPassword(password);
        group.setDescription(description);
        group.setOwnerId(userId);
        group.setMaxMembers(usedCode.getMaxMembers() != null ? usedCode.getMaxMembers() : 100);
        groupService.save(group);
        // 创建者自动加入
        RoomMember member = new RoomMember();
        member.setId(IdUtil.simpleUUID());
        member.setUserId(userId);
        member.setGroupId(roomId);
        member.setRole("member");  // V1 无特殊权限
        member.setJoinTime(new Date());
        roomMemberService.save(member);

        JSONObject result = new JSONObject();
        result.put("roomId", group.getId());
        result.put("name", group.getName());
        result.put("avatar", group.getAvatar());
        result.put("description", group.getDescription());
        result.put("maxMembers", group.getMaxMembers());
        return result;
    }

    @Override
    @Transactional
    public void leaveRoom(String userId, String roomId) {
        LambdaQueryWrapper<RoomMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomMember::getUserId, userId)
               .eq(RoomMember::getGroupId, roomId);
        roomMemberService.remove(wrapper);
    }

    @Override
    public List<JSONObject> listAllRooms() {
        List<Group> groups = groupService.list();
        List<JSONObject> result = new ArrayList<>();
        for (Group group : groups) {
            JSONObject room = new JSONObject();
            room.put("roomId", group.getId());
            room.put("name", group.getName());
            room.put("avatar", group.getAvatar());
            room.put("description", group.getDescription());
            room.put("maxMembers", group.getMaxMembers());
            room.put("password", group.getPassword());
            room.put("ownerId", group.getOwnerId());
            long memberCount = roomMemberService.count(
                    new LambdaQueryWrapper<RoomMember>().eq(RoomMember::getGroupId, group.getId()));
            room.put("memberCount", (int) memberCount);
            int onlineCount = webSocketService.getRoomOnlineCount(group.getId());
            room.put("onlineCount", onlineCount);
            result.add(room);
        }
        return result;
    }

    @Override
    @Transactional
    public void disbandRoom(String roomId) {
        Group group = groupService.getById(roomId);
        if (group == null) {
            throw new MeetLinkException("聊天室不存在~");
        }
        // 通知在线用户并踢下线
        webSocketService.sendDisbandNotify(roomId);
        // 删除 room_member
        LambdaQueryWrapper<RoomMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomMember::getGroupId, roomId);
        roomMemberService.remove(wrapper);
        // 删除 group
        groupService.removeById(roomId);
    }

    @Override
    public void updateRoom(String roomId, String name, String description, String password, Integer maxMembers) {
        Group group = groupService.getById(roomId);
        if (group == null) {
            throw new MeetLinkException("聊天室不存在~");
        }
        if (StrUtil.isNotBlank(name)) {
            group.setName(name);
        }
        if (description != null) {
            group.setDescription(description);
        }
        if (password != null) {
            group.setPassword(password);
        }
        if (maxMembers != null) {
            group.setMaxMembers(maxMembers);
        }
        groupService.updateById(group);
    }
}
