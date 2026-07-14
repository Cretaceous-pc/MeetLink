package com.cheng.meetlink.service;

import cn.hutool.json.JSONObject;
import com.cheng.meetlink.vo.video.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class VideoService {

    @Resource
    WebSocketService webSocketService;

    public boolean offer(String userId, OfferVo offerVo) {
        JSONObject msg = new JSONObject();
        msg.set("type", "offer");
        msg.set("desc", offerVo.getDesc());
        msg.set("fromId", userId);
        webSocketService.sendVideoToUser(msg, offerVo.getUserId());
        return true;
    }

    public boolean answer(String userId, AnswerVo answerVo) {
        JSONObject msg = new JSONObject();
        msg.set("type", "answer");
        msg.set("desc", answerVo.getDesc());
        msg.set("fromId", userId);
        webSocketService.sendVideoToUser(msg, answerVo.getUserId());
        return true;
    }

    public boolean candidate(String userId, CandidateVo candidateVo) {
        JSONObject msg = new JSONObject();
        msg.set("type", "candidate");
        msg.set("candidate", candidateVo.getCandidate());
        msg.set("fromId", userId);
        webSocketService.sendVideoToUser(msg, candidateVo.getUserId());
        return true;
    }

    public boolean hangup(String userId, HangupVo hangupVo) {
        JSONObject msg = new JSONObject();
        msg.set("type", "hangup");
        msg.set("fromId", userId);
        webSocketService.sendVideoToUser(msg, hangupVo.getUserId());
        return true;
    }

    public boolean invite(String userId, InviteVo inviteVo) {
        JSONObject msg = new JSONObject();
        msg.set("type", "invite");
        msg.set("fromId", userId);
        msg.set("isOnlyAudio", inviteVo.isOnlyAudio());
        webSocketService.sendVideoToUser(msg, inviteVo.getUserId());
        return true;
    }

    public boolean accept(String userId, AcceptVo acceptVo) {
        JSONObject msg = new JSONObject();
        msg.set("type", "accept");
        msg.set("fromId", userId);
        webSocketService.sendVideoToUser(msg, acceptVo.getUserId());
        return true;
    }
}
