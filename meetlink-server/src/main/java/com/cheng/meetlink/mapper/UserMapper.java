package com.cheng.meetlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cheng.meetlink.dto.UserDto;
import com.cheng.meetlink.entity.User;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE id = #{userId}")
    @ResultMap("UserDtoResultMap")
    UserDto getUserById(String userId);

    @Select("SELECT * FROM users ORDER BY type DESC")
    @ResultMap("UserDtoResultMap")
    List<UserDto> listUser();

    @Select("SELECT * FROM users ORDER BY type DESC")
    @MapKey("id")
    @ResultMap("UserDtoResultMap")
    Map<String, UserDto> listMapUser();
}
