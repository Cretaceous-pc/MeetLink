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
@TableName(value = "message", autoResultMap = true)
public class Message {
    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一标识
     */
    @TableId("id")
    private String id;

    /**
     * 发送方用户ID
     */
    @TableField("from_id")
    private String fromId;

    /**
     * 接收方用户ID
     */
    @TableField("to_id")
    private String toId;

    /**
     * 发送方用户信息
     */
    @TableField(value = "from_info", typeHandler = JacksonTypeHandler.class)
    private UserDto fromInfo;

    /**
     * 消息内容
     */
    @TableField("message")
    private String message;

    /**
     * 引用的消息信息
     */
    @TableField(value = "reference_msg", typeHandler = JacksonTypeHandler.class)
    private Message referenceMsg;

    /**
     * at的用户信息
     */
    @TableField(value = "at_user", typeHandler = JacksonTypeHandler.class)
    private UserDto userDto;

    /**
     * 是否显示时间（布尔值）
     */
    @TableField("is_show_time")
    private Boolean isShowTime;

    /**
     * 消息类型
     */
    @TableField("type")
    private String type;

    /**
     * 消息来源
     */
    @TableField("source")
    private String source;

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
