package com.cheng.meetlink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cheng.meetlink.entity.Message;
import com.cheng.meetlink.vo.message.RecallVo;
import com.cheng.meetlink.vo.message.RecordVo;
import com.cheng.meetlink.vo.message.SendMessageVo;

import java.time.LocalDate;
import java.util.List;

public interface MessageService extends IService<Message> {
    Message send(String userId, SendMessageVo sendMessageVo);

    List<Message> record(String userId, RecordVo recordVo);

    Message recall(String userId, RecallVo recallVo);

    void deleteExpiredMessages(LocalDate expirationDate);

    Message sendMessageToGroup(String userId, SendMessageVo sendMessageVo);
}
