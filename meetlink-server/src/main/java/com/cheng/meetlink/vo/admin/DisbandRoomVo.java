package com.cheng.meetlink.vo.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DisbandRoomVo {
    @NotBlank(message = "聊天室编号不能为空~")
    private String roomId;
}
