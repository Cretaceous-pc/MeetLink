package com.cheng.meetlink.vo.message;

import lombok.Data;

import javax.validation.constraints.Max;
import java.util.Date;

@Data
public class RecordVo {
    //目标id
    private String targetId;
    //游标：上一条消息的创建时间（首次加载传null）
    private Date cursorTime;
    //游标：上一条消息的ID（首次加载传null）
    private String cursorId;
    //查询条数
    @Max(100)
    private int num;
}
