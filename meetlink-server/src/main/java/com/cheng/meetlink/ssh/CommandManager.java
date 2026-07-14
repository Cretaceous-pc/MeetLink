package com.cheng.meetlink.ssh;

import com.cheng.meetlink.annotation.CommandInfo;
import com.cheng.meetlink.ssh.commands.MessageCommand;
import lombok.Data;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
public class CommandManager {

    private static CommandManager instance;
    private Map<String, CustomCommand> commandMap = new HashMap<>();
    private Map<String, String> detailsMap = new HashMap<>();

    public CommandManager() {
        commandMap.put("message", new MessageCommand());
        loadCommands();
    }

    public void loadCommands() {
        // 使用Reflections库来扫描特定包下的所有类
        Reflections reflections = new Reflections("com.cheng.meetlink.ssh.commands");
        Set<Class<?>> commandClasses = reflections.getTypesAnnotatedWith(CommandInfo.class);
        for (Class<?> clazz : commandClasses) {
            // 获取注解信息
            CommandInfo commandAnnotation = clazz.getAnnotation(CommandInfo.class);
            String name = commandAnnotation.name();
            String description = commandAnnotation.description();
            try {
                // 创建命令类的实例
                CustomCommand commandInstance = (CustomCommand) clazz.getDeclaredConstructor().newInstance();
                // 将命令添加到map中
                commandMap.put(name, commandInstance);
                detailsMap.put(name, description);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void executeCommand(String content, String username) {
        String[] args = content.split(" ");
        CustomCommand command = null;
        if (args.length >= 2) {
            command = commandMap.get(args[0] + " " + args[1]);
        }
        if (command == null) {
            commandMap.get("message").execute(content, username, null, getInstance());
        } else {
            command.execute(content, username, args, getInstance());
        }
    }

    public void systemNotify(String content) {
        MessageCommand message = (MessageCommand) commandMap.get("message");
        message.SystemNotify(content);
    }

    public static CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }
}
