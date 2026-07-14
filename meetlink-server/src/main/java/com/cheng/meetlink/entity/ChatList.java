package com.cheng.meetlink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.cheng.meetlink.dto.UserDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "chat_list", autoResultMap = true)
public class ChatList {
    private static final long serialVersionUID = 1L;

    /**
     * 聊天记录的唯一标识
     */
    @TableId("id")
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 目标用户ID
     */
    @TableField("target_id")
    private String targetId;

    /**
     * 目标用户信息
     */
    @TableField(value = "target_info", typeHandler = JacksonTypeHandler.class)
    private UserDto targetInfo;

    /**
     * 未读消息数
     */
    @TableField("unread_count")
    private Integer unreadCount;

    /**
     * 最后一条消息
     */
    @TableField(value = "last_message", typeHandler = JacksonTypeHandler.class)
    private Message lastMessage;

    /**
     * 聊天类型
     */
    @TableField("type")
    private String type;

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
