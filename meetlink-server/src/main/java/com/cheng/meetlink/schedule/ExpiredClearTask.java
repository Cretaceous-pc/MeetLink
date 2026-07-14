package com.cheng.meetlink.schedule;

import com.cheng.meetlink.service.MessageService;
import com.cheng.meetlink.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;

@Component
public class ExpiredClearTask {

    @Resource
    MessageService messageService;

    @Resource
    UserService userService;

    @Value("${meetlink.expires}")
    int expirationDays;


    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteExpiredContent() {
        LocalDate expirationDate = LocalDate.now().minusDays(expirationDays);
        messageService.deleteExpiredMessages(expirationDate);
        userService.deleteExpiredUsers(expirationDate);
    }
}
