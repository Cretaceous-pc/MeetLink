package com.cheng.meetlink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cheng.meetlink.entity.RoomMember;
import com.cheng.meetlink.mapper.RoomMemberMapper;
import com.cheng.meetlink.service.RoomMemberService;
import org.springframework.stereotype.Service;

@Service
public class RoomMemberServiceImpl extends ServiceImpl<RoomMemberMapper, RoomMember> implements RoomMemberService {
}
