package com.cheng.meetlink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cheng.meetlink.entity.Notify;

public interface NotifyService extends IService<Notify> {
    Notify getLatestNotify();
}
