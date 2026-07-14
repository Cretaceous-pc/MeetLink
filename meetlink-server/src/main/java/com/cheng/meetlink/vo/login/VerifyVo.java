package com.cheng.meetlink.vo.login;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VerifyVo {
    @NotBlank(message = "密码不能为空~")
    private String password;
}
