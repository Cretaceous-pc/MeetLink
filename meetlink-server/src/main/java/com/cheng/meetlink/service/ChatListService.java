package com.cheng.meetlink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cheng.meetlink.entity.ChatList;
import com.cheng.meetlink.entity.Message;

import java.util.List;

public interface ChatListService extends IService<ChatList> {
    List<ChatList> privateList(String userId);

    ChatList getGroup(String userId, String roomId);

    ChatList create(String userId, String targetId);

    boolean updateChatListGroup(Message message);

    boolean updateChatListPrivate(String userId, String targetId, Message message);

    boolean read(String userId, String targetId);

    boolean delete(String userId, String chatListId);
}
