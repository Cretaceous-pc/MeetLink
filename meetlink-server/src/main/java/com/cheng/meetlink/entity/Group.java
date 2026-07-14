package com.cheng.meetlink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("\"group\"")
public class Group {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 用户名
     */
    @TableField("name")
    private String name;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 聊天室密码
     */
    @TableField("password")
    private String password;

    /**
     * 聊天室描述
     */
    @TableField("description")
    private String description;

    /**
     * 人数上限
     */
    @TableField("max_members")
    private Integer maxMembers;

    /**
     * 创建者ID（预留）
     */
    @TableField("owner_id")
    private String ownerId;

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
