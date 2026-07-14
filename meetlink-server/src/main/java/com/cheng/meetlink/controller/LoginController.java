package com.cheng.meetlink.controller;

import cn.hutool.json.JSONObject;
import com.cheng.meetlink.annotation.UrlFree;
import com.cheng.meetlink.annotation.UrlLimit;
import com.cheng.meetlink.constant.LimitKeyType;
import com.cheng.meetlink.service.LoginService;
import com.cheng.meetlink.utils.ResultUtil;
import com.cheng.meetlink.utils.SecurityUtil;
import com.cheng.meetlink.vo.login.LoginVo;
import com.cheng.meetlink.vo.login.VerifyVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/login")
public class LoginController {

    @Resource
    private LoginService loginService;

    @UrlFree
    @PostMapping("/verify")
    @UrlLimit(keyType = LimitKeyType.IP)
    public Object verify(@RequestBody @Valid VerifyVo verifyVo) {
        String result = loginService.verify(verifyVo.getPassword());
        return ResultUtil.Succeed(result);
    }

    @UrlFree
    @GetMapping("/public-key")
    @UrlLimit(keyType = LimitKeyType.IP)
    public Object getPublicKey() {
        String result = SecurityUtil.getPublicKey();
        return ResultUtil.Succeed(result);
    }

    @UrlFree
    @PostMapping("")
    @UrlLimit(keyType = LimitKeyType.IP)
    public Object login(@RequestBody @Valid LoginVo loginVo) {
        JSONObject result = loginService.login(loginVo);
        return ResultUtil.Succeed(result);
    }
}
