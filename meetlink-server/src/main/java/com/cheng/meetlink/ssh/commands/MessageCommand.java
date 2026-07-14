package com.cheng.meetlink.ssh.commands;

import com.cheng.meetlink.ssh.CommandManager;
import com.cheng.meetlink.ssh.CustomCommand;
import com.cheng.meetlink.ssh.InteractionConnect;

public class MessageCommand extends CustomCommand {

    public void broadcast(String message, String username) {
        InteractionConnect connect = ONLINE_USERS.get(username);
        //私信
        if (connect != null && connect.getPrivateChatUserName() != null) {
            for (String user : ONLINE_USERS.keySet()) {
                if (user.equals(connect.getPrivateChatUserName())) {
                    //回显
                    String content = String.format("[%s:%s] %s", username, user, message);
                    sendMsg(content, username, connect, ANSI_PINK);
                    //对方收到私信内容
                    sendMsg(content, user, ONLINE_USERS.get(user), ANSI_PINK);
                    break;
                }
            }
        } else {
            ONLINE_USERS.values().forEach(cmd -> {
                boolean isCurrentUser = username.equals(cmd.getUsername());
                String content = String.format("%s: %s", username, message);
                String color = isCurrentUser ? ANSI_GREEN : ANSI_RESET;
                sendMsg(content, cmd.getUsername(), cmd, color);
            });
        }
    }

    private void sendMsg(String message, String username, InteractionConnect connect, String color) {
        connect.getWriter().print("\r\033[K");
        connect.getWriter().println(color + message);
        echo(username, connect);
    }

    public void SystemNotify(String message) {
        ONLINE_USERS.values().forEach(cmd -> {
            sendMsg(message, cmd.getUsername(), cmd, ANSI_BLUE);
        });
    }

    @Override
    public void execute(String message, String username, String[] args, CommandManager commandManager) {
        broadcast(message, username);
    }
}
