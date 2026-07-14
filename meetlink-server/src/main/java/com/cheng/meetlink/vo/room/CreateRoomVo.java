package com.cheng.meetlink.vo.room;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateRoomVo {
    @NotBlank(message = "聊天室编号不能为空~")
    private String roomId;

    @NotBlank(message = "聊天室密码不能为空~")
    private String password;

    @NotBlank(message = "聊天室名称不能为空~")
    private String name;

    @NotBlank(message = "邀请码不能为空~")
    private String inviteCode;

    private String description;
}
