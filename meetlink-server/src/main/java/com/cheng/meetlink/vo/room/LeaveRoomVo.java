package com.cheng.meetlink.vo.room;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LeaveRoomVo {
    @NotBlank(message = "聊天室编号不能为空~")
    private String roomId;
}
