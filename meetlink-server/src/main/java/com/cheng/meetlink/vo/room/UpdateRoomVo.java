package com.cheng.meetlink.vo.room;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateRoomVo {
    @NotBlank(message = "聊天室编号不能为空~")
    private String roomId;

    private String name;
    private String description;
    private String password;
    private Integer maxMembers;
}
