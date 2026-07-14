package com.cheng.meetlink.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cheng.meetlink.constant.ChatListType;
import com.cheng.meetlink.dto.UserDto;
import com.cheng.meetlink.entity.ChatList;
import com.cheng.meetlink.entity.Group;
import com.cheng.meetlink.entity.Message;
import com.cheng.meetlink.mapper.ChatListMapper;
import com.cheng.meetlink.service.ChatListService;
import com.cheng.meetlink.service.GroupService;
import com.cheng.meetlink.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ChatListServiceImpl extends ServiceImpl<ChatListMapper, ChatList> implements ChatListService {

    @Resource
    @Lazy
    UserService userService;

    @Resource
    GroupService groupService;

    @Override
    public List<ChatList> privateList(String userId) {
        LambdaQueryWrapper<ChatList> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatList::getUserId, userId)
                .eq(ChatList::getType, ChatListType.User);
        return list(queryWrapper);
    }

    @Override
    public ChatList getGroup(String userId, String roomId) {
        LambdaQueryWrapper<ChatList> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatList::getUserId, userId)
                .eq(ChatList::getType, ChatListType.Group)
                .eq(ChatList::getTargetId, roomId);
        ChatList chat = getOne(queryWrapper);
        Group group = groupService.getById(roomId);
        String groupName = group != null ? group.getName() : "聊天室";
        String groupAvatar = group != null ? group.getAvatar() : null;
        if (chat == null) {
            chat = new ChatList();
            chat.setId(IdUtil.simpleUUID());
            chat.setType(ChatListType.Group);
            chat.setUserId(userId);
            chat.setTargetId(roomId);
            UserDto userDto = new UserDto();
            userDto.setId(roomId);
            userDto.setName(groupName);
            userDto.setAvatar(groupAvatar);
            chat.setTargetInfo(userDto);
            save(chat);
        } else {
            // 每次获取时同步最新的群名称和头像
            UserDto targetInfo = chat.getTargetInfo();
            if (targetInfo != null &&
                (!groupName.equals(targetInfo.getName()) ||
                 (groupAvatar != null && !groupAvatar.equals(targetInfo.getAvatar())))) {
                targetInfo.setName(groupName);
                targetInfo.setAvatar(groupAvatar);
                chat.setTargetInfo(targetInfo);
                updateById(chat);
            }
        }
        return chat;
    }

    public ChatList getTargetChatList(String userId, String targetId) {
        LambdaQueryWrapper<ChatList> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatList::getTargetId, targetId)
                .eq(ChatList::getUserId, userId)
                .eq(ChatList::getType, ChatListType.User);
        return getOne(queryWrapper);
    }

    @Override
    public ChatList create(String userId, String targetId) {
        if (userId.equals(targetId))
            return null;
        ChatList targetChatList = getTargetChatList(userId, targetId);
        if (targetChatList != null) {
            return targetChatList;
        }
        UserDto user = userService.getUserById(targetId);
        ChatList chatList = new ChatList();
        chatList.setId(IdUtil.simpleUUID());
        chatList.setUserId(userId);
        chatList.setTargetId(targetId);
        chatList.setType(ChatListType.User);
        chatList.setTargetInfo(user);
        chatList.setLastMessage(new Message());
        save(chatList);
        return chatList;
    }

    @Override
    public boolean updateChatListGroup(Message message) {
        LambdaUpdateWrapper<ChatList> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(ChatList::getLastMessage, JSONUtil.toJsonStr(message))
                .eq(ChatList::getType, ChatListType.Group)
                .eq(ChatList::getTargetId, message.getToId());
        return update(updateWrapper);
    }

    public boolean updateChatList(String userId, String targetId, Message message) {
        //判断聊天列表是否存在
        LambdaQueryWrapper<ChatList> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatList::getUserId, targetId)
                .eq(ChatList::getTargetId, userId);
        ChatList chatList = getOne(queryWrapper);
        if (null == chatList) {
            chatList = new ChatList();
            chatList.setId(IdUtil.randomUUID());
            chatList.setUserId(targetId);
            chatList.setType(ChatListType.User);
            chatList.setTargetId(userId);
            chatList.setUnreadCount(1);
            chatList.setTargetInfo(userService.getUserById(userId));
            chatList.setLastMessage(message);
            return save(chatList);
        } else {
            chatList.setUnreadCount(chatList.getUnreadCount() + 1);
            chatList.setLastMessage(message);
            return updateById(chatList);
        }
    }

    @Override
    public boolean updateChatListPrivate(String userId, String targetId, Message message) {
        updateChatList(targetId, userId, message);
        //更新自己的聊天列表
        return updateChatList(userId, targetId, message);
    }

    @Override
    public boolean read(String userId, String targetId) {
        if (targetId == null) return false;
        LambdaUpdateWrapper<ChatList> updateWrapper = new LambdaUpdateWrapper();
        updateWrapper.set(ChatList::getUnreadCount, 0)
                .eq(ChatList::getUserId, userId)
                .eq(ChatList::getTargetId, targetId);
        return update(new ChatList(), updateWrapper);
    }

    @Override
    public boolean delete(String userId, String chatListId) {
        return removeById(chatListId);
    }
}
