package com.cheng.meetlink.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cheng.meetlink.constant.WsContentType;
import com.cheng.meetlink.dto.NotifyDto;
import com.cheng.meetlink.entity.Message;
import com.cheng.meetlink.utils.CacheUtil;
import com.cheng.meetlink.utils.JwtUtil;
import com.cheng.meetlink.utils.ResultUtil;
import io.jsonwebtoken.Claims;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Data;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketService {

    @Data
    public static class WsContent {
        private String type;
        private Object content;
    }

    // 房间解散通知类型
    public static final String DisbandType = "disband";

    @Resource
    @Lazy
    UserService userService;

    @Resource
    CacheUtil cacheUtil;

    // userId → Channel
    public static final ConcurrentHashMap<String, Channel> Online_User = new ConcurrentHashMap<>();
    // Channel → userId
    public static final ConcurrentHashMap<Channel, String> Online_Channel = new ConcurrentHashMap<>();
    // roomId → Set<userId>  房间内在线用户
    public static final ConcurrentHashMap<String, Set<String>> Room_Users = new ConcurrentHashMap<>();
    // userId → roomId  用户当前所在房间
    public static final ConcurrentHashMap<String, String> User_Room = new ConcurrentHashMap<>();

    public void online(Channel channel, String token, String roomId) {
        try {
            Claims claims = JwtUtil.parseToken(token);
            String userId = (String) claims.get("userId");
            String cacheToken = cacheUtil.getUserSessionCache(userId);
            if (!token.equals(cacheToken)) {
                sendMsg(channel, ResultUtil.Fail("已在其他地方登录"), WsContentType.Msg);
                channel.close();
                return;
            }
            // 如果已在其他房间在线，先清理旧连接
            String oldRoomId = User_Room.get(userId);
            if (oldRoomId != null) {
                Set<String> oldRoomUsers = Room_Users.get(oldRoomId);
                if (oldRoomUsers != null) {
                    oldRoomUsers.remove(userId);
                }
            }
            Online_User.put(userId, channel);
            Online_Channel.put(channel, userId);
            User_Room.put(userId, roomId);
            Room_Users.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
            userService.online(userId);
        } catch (Exception e) {
            sendMsg(channel, ResultUtil.Fail("连接错误"), WsContentType.Msg);
            channel.close();
        }
    }

    public void offline(Channel channel) {
        String userId = Online_Channel.get(channel);
        if (StrUtil.isNotBlank(userId)) {
            Online_User.remove(userId);
            Online_Channel.remove(channel);
            String roomId = User_Room.remove(userId);
            if (roomId != null) {
                Set<String> roomUsers = Room_Users.get(roomId);
                if (roomUsers != null) {
                    roomUsers.remove(userId);
                }
            }
            userService.offline(userId);
        }
    }

    private void sendMsg(Channel channel, Object msg, String type) {
        WsContent wsContent = new WsContent();
        wsContent.setType(type);
        wsContent.setContent(msg);
        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(wsContent)));
    }

    public void sendMsgToUser(Object msg, String userId, String targetId) {
        Channel channel = Online_User.get(userId);
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Msg);
        }
        channel = Online_User.get(targetId);
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Msg);
        }
    }

    /**
     * 发送消息到房间（仅同房间在线用户）
     */
    public void sendMsgToGroup(String roomId, Message message) {
        Set<String> userIds = Room_Users.get(roomId);
        if (userIds != null) {
            userIds.forEach(userId -> {
                Channel channel = Online_User.get(userId);
                if (channel != null) {
                    sendMsg(channel, message, WsContentType.Msg);
                }
            });
        }
    }

    public Integer getOnlineNum() {
        return Online_User.size();
    }

    /**
     * 获取某个房间的在线人数
     */
    public int getRoomOnlineCount(String roomId) {
        Set<String> userIds = Room_Users.get(roomId);
        return userIds != null ? userIds.size() : 0;
    }

    /**
     * 获取用户当前所在房间
     */
    public String getUserRoom(String userId) {
        return User_Room.get(userId);
    }

    public List<String> getOnlineUser() {
        return new ArrayList<>(Online_User.keySet());
    }

    /**
     * 发送通知到房间
     */
    public void sendNotifyToGroup(String roomId, NotifyDto notify) {
        Set<String> userIds = Room_Users.get(roomId);
        if (userIds != null) {
            userIds.forEach(userId -> {
                Channel channel = Online_User.get(userId);
                if (channel != null) {
                    sendMsg(channel, notify, WsContentType.Notify);
                }
            });
        }
    }

    /**
     * 解散房间通知（通知后踢掉在线用户）
     */
    public void sendDisbandNotify(String roomId) {
        Set<String> userIds = Room_Users.get(roomId);
        if (userIds != null) {
            userIds.forEach(userId -> {
                Channel channel = Online_User.get(userId);
                if (channel != null) {
                    sendMsg(channel, ResultUtil.Succeed("聊天室已被管理员解散"), DisbandType);
                    channel.close();
                }
            });
        }
    }

    public void sendVideoToUser(Object msg, String userId) {
        Channel channel = Online_User.get(userId);
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Video);
        }
    }

    public void sendFileToUser(Object msg, String userId) {
        Channel channel = Online_User.get(userId);
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.File);
        }
    }
}
