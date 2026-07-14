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
@TableName("invite_code")
public class InviteCode {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 邀请码
     */
    @TableField("code")
    private String code;

    /**
     * 人数上限（创建房间时写入）
     */
    @TableField("max_members")
    private Integer maxMembers;

    /**
     * 是否已使用
     */
    @TableField("is_used")
    private Boolean isUsed;

    /**
     * 使用者用户ID
     */
    @TableField("used_by")
    private String usedBy;

    /**
     * 创建的房间ID
     */
    @TableField("used_for_group")
    private String usedForGroup;

    /**
     * 签发管理员ID
     */
    @TableField("created_by")
    private String createdBy;

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
