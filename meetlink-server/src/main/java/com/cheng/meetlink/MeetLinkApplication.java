package com.cheng.meetlink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.cheng.meetlink.mapper")
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MeetLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetLinkApplication.class, args);
    }

}
