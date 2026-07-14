package com.cheng.meetlink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cheng.meetlink.entity.Group;

public interface GroupService extends IService<Group> {
    void updateDefaultGroup();
}
