package com.cheng.meetlink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cheng.meetlink.entity.Group;
import com.cheng.meetlink.mapper.GroupMapper;
import com.cheng.meetlink.service.GroupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {
    @Value("${meetlink.name}")
    private String defaultGroupName;

    @Value("${meetlink.password}")
    private String defaultPassword;

    @Override
    public void updateDefaultGroup() {
        // 如果已有聊天室则跳过（由管理员通过管理面板管理）
        if (count() > 0) return;
        // 否则创建默认聊天室 001
        Group group = new Group();
        group.setId("001");
        group.setName(defaultGroupName);
        group.setPassword(defaultPassword);
        group.setDescription("系统默认聊天室，欢迎加入");
        group.setMaxMembers(200);
        save(group);
    }
}
