package com.cheng.meetlink.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.cheng.meetlink.constant.UserType;
import com.cheng.meetlink.entity.RoomMember;
import com.cheng.meetlink.entity.User;
import com.cheng.meetlink.exception.MeetLinkException;
import com.cheng.meetlink.service.RoomMemberService;
import com.cheng.meetlink.utils.CacheUtil;
import com.cheng.meetlink.utils.JwtUtil;
import com.cheng.meetlink.utils.SecurityUtil;
import com.cheng.meetlink.vo.login.LoginVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoginService {

    @Resource
    UserService userService;

    @Resource
    RoomMemberService roomMemberService;

    @Value("${meetlink.password}")
    private String meetlinkPassword;

    @Value("${meetlink.limit}")
    private int meetlinkLimit;

    @Value("${meetlink.admin-email:}")
    private String adminEmail;

    @Resource
    CacheUtil cacheUtil;

    @Resource
    WebSocketService webSocketService;

    public String verify(String password) {
        if (webSocketService.getOnlineNum() >= meetlinkLimit) {
            throw new MeetLinkException("聊天室人数已满，请稍后再试~");
        }
        String decryptedPassword = SecurityUtil.decryptPassword(password);
        if (!meetlinkPassword.equals(decryptedPassword)) {
            throw new MeetLinkException("密码错误~");
        }
        Map tokenInfo = new HashMap<String, String>();
        tokenInfo.put("type", "verify");
        return JwtUtil.createToken(tokenInfo);
    }

    public JSONObject login(LoginVo loginVo) {
        if (webSocketService.getOnlineNum() >= meetlinkLimit) {
            throw new MeetLinkException("聊天室人数已满，请稍后再试~");
        }
        User user = userService.getUserByNameOrEmail(loginVo.getName(), loginVo.getEmail());
        if (user != null) {
            if (loginVo.getName().equals(user.getName()) &&
                    !loginVo.getEmail().equals(user.getEmail())) {
                throw new MeetLinkException("用户名已被使用~");
            }
            if (!loginVo.getName().equals(user.getName()) &&
                    loginVo.getEmail().equals(user.getEmail())) {
                throw new MeetLinkException("邮箱已被使用~");
            }
            user.setLoginTime(new Date());
            // 管理员检测
            if (StrUtil.isNotBlank(adminEmail) && adminEmail.equals(user.getEmail())) {
                user.setRole("admin");
            }
            userService.updateById(user);
        } else {
            user = new User();
            user.setId(IdUtil.simpleUUID());
            user.setName(loginVo.getName());
            user.setEmail(loginVo.getEmail());
            user.setLoginTime(new Date());
            user.setType(UserType.User);
            // 管理员检测
            if (StrUtil.isNotBlank(adminEmail) && adminEmail.equals(loginVo.getEmail())) {
                user.setRole("admin");
            }
            userService.save(user);
            // 新用户自动加入默认聊天室 001
            RoomMember member = new RoomMember();
            member.setId(IdUtil.simpleUUID());
            member.setUserId(user.getId());
            member.setGroupId("001");
            member.setRole("member");
            member.setJoinTime(new Date());
            roomMemberService.save(member);
        }
        JSONObject userinfo = new JSONObject();
        userinfo.put("type", "user");
        userinfo.put("userId", user.getId());
        userinfo.put("userName", user.getName());
        userinfo.put("email", user.getEmail());
        userinfo.put("avatar", user.getAvatar());
        userinfo.put("role", user.getRole());
        String token = JwtUtil.createToken(userinfo);
        userinfo.put("token", token);
        cacheUtil.putUserSessionCache(user.getId(), token);
        //更新用户徽章
        userService.updateUserBadge(user.getId());
        return userinfo;
    }
}
