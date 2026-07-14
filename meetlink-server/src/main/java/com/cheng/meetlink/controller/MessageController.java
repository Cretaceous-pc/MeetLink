package com.cheng.meetlink.controller;

import com.cheng.meetlink.annotation.UrlLimit;
import com.cheng.meetlink.annotation.UserIp;
import com.cheng.meetlink.annotation.Userid;
import com.cheng.meetlink.entity.Message;
import com.cheng.meetlink.service.MessageService;
import com.cheng.meetlink.utils.ResultUtil;
import com.cheng.meetlink.vo.message.RecallVo;
import com.cheng.meetlink.vo.message.RecordVo;
import com.cheng.meetlink.vo.message.SendMessageVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/message")
public class MessageController {

    @Resource
    MessageService messageService;

    @UrlLimit(maxRequests = 100)
    @PostMapping("/send")
    public Object send(@Userid String userId, @UserIp String userIp,
                       @RequestBody @Valid SendMessageVo sendMessageVo) {
        sendMessageVo.setUserIp(userIp);
        Message result = messageService.send(userId, sendMessageVo);
        return ResultUtil.Succeed(result);
    }

    @UrlLimit
    @PostMapping("/record")
    public Object record(@Userid String userId, @RequestBody @Valid RecordVo recordVo) {
        List<Message> result = messageService.record(userId, recordVo);
        return ResultUtil.Succeed(result);
    }

    @UrlLimit
    @PostMapping("/recall")
    public Object recall(@Userid String userId, @RequestBody @Valid RecallVo recallVo) {
        Message result = messageService.recall(userId, recallVo);
        return ResultUtil.Succeed(result);
    }
}
