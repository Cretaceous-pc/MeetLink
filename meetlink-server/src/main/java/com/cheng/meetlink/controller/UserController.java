package com.cheng.meetlink.controller;

import com.cheng.meetlink.annotation.UrlLimit;
import com.cheng.meetlink.annotation.Userid;
import com.cheng.meetlink.dto.UserDto;
import com.cheng.meetlink.service.UserService;
import com.cheng.meetlink.utils.ResultUtil;
import com.cheng.meetlink.vo.user.UpdateUserVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Resource
    UserService userService;

    @UrlLimit
    @GetMapping("/list")
    public Object listUser() {
        List<UserDto> result = userService.listUser();
        return ResultUtil.Succeed(result);
    }

    @UrlLimit
    @GetMapping("/list/map")
    public Object listMapUser() {
        Map<String, UserDto> result = userService.listMapUser();
        return ResultUtil.Succeed(result);
    }

    @UrlLimit
    @GetMapping("/online/web")
    public Object onlineWeb() {
        List<String> result = userService.onlineWeb();
        return ResultUtil.Succeed(result);
    }

    @UrlLimit
    @PostMapping("/update")
    public Object updateUser(@Userid String userid, @RequestBody @Valid UpdateUserVo updateUserVo) {
        boolean result = userService.updateUser(userid, updateUserVo);
        return ResultUtil.ResultByFlag(result);
    }
}
