package com.cheng.meetlink.ssh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CustomCommand {
    public static final Map<String, InteractionConnect> ONLINE_USERS = new ConcurrentHashMap<>();
    public static final String ANSI_RESET = "\033[0m";
    public static final String ANSI_RED = "\033[38;2;255;76;76m";
    public static final String ANSI_GREEN = "\033[38;2;105;189;68m";
    public static final String ANSI_BLUE = "\033[38;2;76;155;255m";
    public static final String ANSI_YELLOW = "\033[38;2;255;184;0m";
    public static final String ANSI_YELLOW2 = "\033[38;2;255;145;99m";
    public static final String ANSI_PINK = "\033[38;2;255;160;207m";

    public abstract void execute(String content, String username, String[] args, CommandManager commandManager);

    public void echo(String username, InteractionConnect connect) {
        boolean isCurrentUser = username.equals(connect.getUsername());
        InteractionConnect currentConnect = ONLINE_USERS.get(username);
        String title = "[群聊]";
        if (connect != null) {
            String privateChatUserName = currentConnect.getPrivateChatUserName();
            if (privateChatUserName != null && privateChatUserName.length() > 0) {
                title = String.format("[私聊:%s]", privateChatUserName);
            }
        }
        if (!isCurrentUser) {
            connect.getWriter().printf(ANSI_RESET + title + " > %s", connect.getCurrentInput().toString());
        } else {
            connect.getWriter().printf(ANSI_RESET + title + " > ");
        }
    }

    public void error(InteractionConnect connect, String message) {
        connect.getWriter().println(ANSI_RED + "[错误] " + message + ANSI_RESET);
        connect.getWriter().flush();
    }

}
