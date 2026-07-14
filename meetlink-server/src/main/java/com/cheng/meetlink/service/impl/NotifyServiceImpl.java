package com.cheng.meetlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cheng.meetlink.entity.Notify;
import com.cheng.meetlink.mapper.NotifyMapper;
import com.cheng.meetlink.service.NotifyService;
import org.springframework.stereotype.Service;

@Service
public class NotifyServiceImpl extends ServiceImpl<NotifyMapper, Notify> implements NotifyService {

    @Override
    public Notify getLatestNotify() {
        LambdaQueryWrapper<Notify> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Notify::getCreateTime)
                .last("LIMIT 1");
        Notify latestNotify = getOne(queryWrapper);
        return latestNotify;
    }
}
