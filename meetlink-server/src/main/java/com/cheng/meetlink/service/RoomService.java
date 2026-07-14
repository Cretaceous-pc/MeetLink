package com.cheng.meetlink.service;

import cn.hutool.json.JSONObject;

import java.util.List;

public interface RoomService {
    /**
     * 获取用户已加入的聊天室列表
     */
    List<JSONObject> listUserRooms(String userId);

    /**
     * 加入聊天室（编号+密码验证）
     */
    JSONObject joinRoom(String userId, String roomId, String password);

    /**
     * 创建聊天室（自定义编号+密码+基本信息+邀请码验证）
     */
    JSONObject createRoom(String userId, String roomId, String password, String name, String inviteCode, String description);

    /**
     * 退出聊天室
     */
    void leaveRoom(String userId, String roomId);

    /**
     * 获取所有聊天室（管理员）
     */
    List<JSONObject> listAllRooms();

    /**
     * 解散聊天室（管理员）
     */
    void disbandRoom(String roomId);

    /**
     * 更新聊天室信息（管理员）
     */
    void updateRoom(String roomId, String name, String description, String password, Integer maxMembers);
}
