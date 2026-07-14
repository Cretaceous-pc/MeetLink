package com.cheng.meetlink.exception;


import cn.hutool.json.JSONUtil;
import com.cheng.meetlink.utils.ResultUtil;

import java.util.HashMap;

/**
 **/
public class MeetLinkException extends RuntimeException {

    private int code;
    private String message;
    private HashMap<String, Object> param;

    public MeetLinkException(String message) {
        this.code = ResultUtil.ResponseEnum.FAIL.getType();
        this.message = message;
    }

    /***
     * 添加异常信息 键值对
     */
    public MeetLinkException param(String key, Object value) {
        if (null == this.param) {
            this.param = new HashMap<>();
        }
        param.put(key, value);
        return this;
    }

    /***
     * 置空param
     */
    public MeetLinkException empty() {
        this.param = new HashMap<>();
        return this;
    }

    public MeetLinkException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String paramToString() {
        if (null == this.param || this.param.size() <= 0)
            return null;
        return JSONUtil.toJsonStr(this.param);
    }
}
