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
@TableName("room_member")
public class RoomMember {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 聊天室ID
     */
    @TableField("group_id")
    private String groupId;

    /**
     * 角色: owner / member
     */
    @TableField("role")
    private String role;

    /**
     * 禁言截止时间（V2 预留）
     */
    @TableField("muted_until")
    private Date mutedUntil;

    /**
     * 加入时间
     */
    @TableField("join_time")
    private Date joinTime;

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
