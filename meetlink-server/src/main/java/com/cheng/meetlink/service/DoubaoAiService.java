package com.cheng.meetlink.service;


import cn.hutool.core.util.StrUtil;
import com.cheng.meetlink.configs.MeetLinkConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DoubaoAiService {

    static ArkService service;

    private final Cache<String, AtomicInteger> limitCache;

    @Resource
    MeetLinkConfig config;

    DoubaoAiService() {
        this.limitCache = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build();
    }

    @PostConstruct
    public void init() {
        String apiKey = config.getDoubao().getApiKey();
        service = ArkService.builder()
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .apiKey(apiKey).build();
    }

    public String ask(String userId, String content) {
        AtomicInteger count = limitCache.getIfPresent(userId);
        if (count == null) {
            count = new AtomicInteger(0);
            limitCache.put(userId, count);
        }
        if (config.getDoubao().getCountLimit() > 0
                && count.incrementAndGet() > config.getDoubao().getCountLimit()) {
            return "您已经达到限制了，请24小时后再来吧~";
        }
        if (StrUtil.isBlank(content)) return "内容不能为空~";
        if (config.getDoubao().getLengthLimit() > 0 &&
                content.length() > config.getDoubao().getLengthLimit()) {
            return "问一些简单的问题吧~";
        }
        try {
            final List<ChatMessage> messages = new ArrayList<>();
            final ChatMessage userMessage = ChatMessage.builder().
                    role(ChatMessageRole.USER).content(content).build();
            messages.add(userMessage);
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    .model(config.getDoubao().getModel())
                    .messages(messages)
                    .build();
            StringBuffer sb = new StringBuffer();
            service.createChatCompletion(chatCompletionRequest).getChoices().forEach(choice -> sb.append(choice.getMessage().getContent()).append("\n"));
            return sb.toString();
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("timeout") || msg.contains("Timeout") || msg.contains("超时"))) {
                System.err.println("[豆包] 请求超时: " + msg);
                return "服务超时，请联系管理员";
            }
            return "豆包已离家出走了，请稍后再试~";
        }
    }
}
