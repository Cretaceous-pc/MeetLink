package com.cheng.meetlink.ssh.commands;

import com.cheng.meetlink.annotation.CommandInfo;
import com.cheng.meetlink.ssh.CommandManager;
import com.cheng.meetlink.ssh.CustomCommand;
import com.cheng.meetlink.ssh.InteractionConnect;


@CommandInfo(name = "meetlink help", description = "获取meetlink命令列表及其用法")
public class LinyuHelpCommand extends CustomCommand {

    @Override
    public void execute(String content, String username, String[] args, CommandManager commandManager) {
        InteractionConnect connect = ONLINE_USERS.get(username);
        connect.getWriter().println(ANSI_YELLOW + "[meetlink命令列表]");
        commandManager.getDetailsMap().forEach((name, info) -> {
            connect.getWriter().print(ANSI_YELLOW + name + " - ");
            connect.getWriter().println(ANSI_YELLOW2 + info);
        });
        connect.getWriter().print(ANSI_RESET);
        echo(username, connect);
    }
}
