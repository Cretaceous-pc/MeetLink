package com.cheng.meetlink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cheng.meetlink.dto.UserDto;
import com.cheng.meetlink.entity.User;
import com.cheng.meetlink.vo.user.UpdateUserVo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {
    boolean isExist(String name, String email);

    User getUserByNameOrEmail(String name, String email);

    User getUserByName(String name);

    UserDto getUserById(String userId);

    List<UserDto> listUser();

    List<String> onlineWeb();

    Map<String, UserDto> listMapUser();

    void online(String userId);

    void offline(String userId);

    void deleteExpiredUsers(LocalDate expirationDate);

    void updateUserBadge(String id);

    void initBotUser();

    boolean updateUser(String userid, UpdateUserVo updateUserVo);
}
