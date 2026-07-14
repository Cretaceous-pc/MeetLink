package com.cheng.meetlink.ssh;

import lombok.Data;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Data
public class InteractionConnect implements Command, Runnable {
    private InputStream inputStream;
    private OutputStream outputStream;
    private OutputStream errorStream;
    private ExitCallback exitCallback;
    private Thread thread;
    private String username;
    private BufferedReader reader;
    private PrintWriter writer;
    private StringBuilder currentInput = new StringBuilder();
    private CommandManager commandManager = CommandManager.getInstance();
    private String privateChatUserName = null;

    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);
    }

    @Override
    public void setErrorStream(OutputStream errorStream) {
        this.errorStream = errorStream;
    }

    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.exitCallback = exitCallback;
    }

    @Override
    public void start(ChannelSession channelSession, Environment environment) {
        environment.getEnv().put(Environment.ENV_TERM, "vt100");
        this.username = channelSession.getSession().getUsername();
        CustomCommand.ONLINE_USERS.put(username, this);
        writer.println(CustomCommand.ANSI_YELLOW + "欢迎来到MeetLink聊天室！");
        writer.print(CustomCommand.ANSI_YELLOW + "当前在线用户: ");
        writer.println(CustomCommand.ANSI_BLUE + String.join(", ",
                CustomCommand.ONLINE_USERS.keySet()) + CustomCommand.ANSI_RESET);
        writer.println(CustomCommand.ANSI_YELLOW + "输入 'quit' 退出聊天室");
        commandManager.systemNotify(String.format("[系统] %s 加入了聊天室", username));
        writer.flush();
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void destroy(ChannelSession channelSession) {
        CustomCommand.ONLINE_USERS.remove(username);
        commandManager.systemNotify(String.format("[系统] %s 离开了聊天室"));
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void run() {
        try {
            int read;
            int cursorPosition = 0;  // 字符位置
            int displayPosition = 0;  // 显示位置
            while ((read = this.reader.read()) != -1) {
                char ch = (char) read;
                // 处理ANSI转义序列
                if (ch == '\033') {  // ESC字符
                    read = this.reader.read();
                    if (read == '[') {  // CSI序列
                        read = this.reader.read();
                        switch (read) {
                            case 'C':  // 右方向键
                                if (cursorPosition < currentInput.length()) {
                                    // 获取下一个字符的宽度
                                    String nextChar = String.valueOf(currentInput.charAt(cursorPosition));
                                    int charWidth = getCharDisplayWidth(nextChar);
                                    writer.print("\033[" + charWidth + "C");  // 向右移动光标
                                    cursorPosition++;
                                    displayPosition += charWidth;
                                    writer.flush();
                                }
                                continue;
                            case 'D':  // 左方向键
                                if (cursorPosition > 0) {
                                    // 获取前一个字符的宽度
                                    String prevChar = String.valueOf(currentInput.charAt(cursorPosition - 1));
                                    int charWidth = getCharDisplayWidth(prevChar);
                                    writer.print("\033[" + charWidth + "D");  // 向左移动光标
                                    cursorPosition--;
                                    displayPosition -= charWidth;
                                    writer.flush();
                                }
                                continue;
                        }
                    }
                    continue;
                }

                // 处理回车键
                if (ch == '\n' || ch == '\r') {
                    String line = currentInput.toString();
                    if ("quit".equalsIgnoreCase(line.trim())) {
                        writer.println("\r再见！");
                        exitCallback.onExit(0);
                        break;
                    }
                    if (!line.trim().isEmpty()) {
                        writer.print("\r\033[K");
                        commandManager.executeCommand(line, username);
                    }
                    currentInput.setLength(0);
                    cursorPosition = 0;
                    displayPosition = 0;
                    writer.flush();
                    continue;
                }

                // 处理退格键
                if (ch == '\b' || ch == '\u007f') {
                    if (cursorPosition > 0) {
                        // 获取要删除的字符
                        String deletedChar = String.valueOf(currentInput.charAt(cursorPosition - 1));
                        int charWidth = getCharDisplayWidth(deletedChar);

                        // 删除字符
                        currentInput.deleteCharAt(cursorPosition - 1);
                        cursorPosition--;
                        displayPosition -= charWidth;

                        // 重新显示从光标位置到末尾的所有字符
                        writer.print(repeatChar('\b', charWidth));// 向左移动
                        writer.print("\033[K");  // 清除从光标到行尾的内容
                        String remaining = currentInput.substring(cursorPosition);
                        writer.print(remaining);

                        // 计算剩余文本的显示宽度
                        int remainingWidth = getStringDisplayWidth(remaining);

                        // 将光标移回正确的位置
                        if (remainingWidth > 0) {
                            writer.print("\033[" + remainingWidth + "D");
                        }
                        writer.flush();
                    }
                    continue;
                }

                // 忽略不可打印字符
                if (Character.isISOControl(ch)) {
                    continue;
                }

                // 在光标位置插入字符
                String newChar = String.valueOf(ch);
                int charWidth = getCharDisplayWidth(newChar);

                if (cursorPosition == currentInput.length()) {
                    currentInput.append(ch);
                    writer.print(newChar);
                    cursorPosition++;
                    displayPosition += charWidth;
                } else {
                    currentInput.insert(cursorPosition, ch);
                    // 显示从插入位置到末尾的所有字符
                    String restContent = currentInput.substring(cursorPosition);
                    writer.print(restContent);

                    // 计算需要移动的显示宽度
                    int restWidth = getStringDisplayWidth(restContent);
                    cursorPosition++;
                    displayPosition += charWidth;

                    // 将光标移回正确的位置
                    writer.print("\033[" + (restWidth - charWidth) + "D");
                }
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            exitCallback.onExit(1, e.getMessage());
        }
    }

    // 获取单个字符的显示宽度
    private int getCharDisplayWidth(String ch) {
        if (ch.matches("[\\u4e00-\\u9fa5]")) {
            return 2;  // 中文字符占用两个宽度
        }
        return 1;  // ASCII字符占用一个宽度
    }

    // 获取字符串的总显示宽度
    private int getStringDisplayWidth(String str) {
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            width += getCharDisplayWidth(String.valueOf(str.charAt(i)));
        }
        return width;
    }

    private static String repeatChar(char ch, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(ch);
        }
        return builder.toString();
    }
}