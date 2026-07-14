package com.cheng.meetlink.controller;

import com.cheng.meetlink.annotation.UrlLimit;
import com.cheng.meetlink.entity.Notify;
import com.cheng.meetlink.service.NotifyService;
import com.cheng.meetlink.utils.ResultUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/v1/notify")
public class NotifyController {

    @Resource
    NotifyService notifyService;

    @UrlLimit
    @GetMapping("/get")
    public Object getLatestNotify() {
        Notify result = notifyService.getLatestNotify();
        return ResultUtil.Succeed(result);
    }

}
