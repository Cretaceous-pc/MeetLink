package com.cheng.meetlink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "users", autoResultMap = true)
public class User {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 用户名
     */
    @TableField("name")
    private String name;


    /**
     * 用户类型
     */
    @TableField("type")
    private String type;


    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 徽章
     */
    @TableField(value = "badge", typeHandler = JacksonTypeHandler.class)
    private List<String> badge;

    /**
     * 角色: user / admin
     */
    @TableField("role")
    private String role;

    /**
     * 登录时间
     */
    @TableField(value = "login_time")
    private Date loginTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
