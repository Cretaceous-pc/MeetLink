package com.cheng.meetlink.annotation;

import com.cheng.meetlink.constant.LimitKeyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlLimit {
    LimitKeyType keyType() default LimitKeyType.ID; //限制类型，ip或者id

    int maxRequests() default 60; //每分钟最大请求次数
}
