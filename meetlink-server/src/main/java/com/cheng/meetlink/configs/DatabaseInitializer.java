package com.cheng.meetlink.configs;
import com.cheng.meetlink.service.GroupService;
import com.cheng.meetlink.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//数据库初始化组件
@Component
public class DatabaseInitializer {

    /**
     * JdbcTemplate：Spring提供的JDBC操作工具类，简化数据库CRUD操作
     * 使用@Resource注解自动注入Spring容器中的JdbcTemplate实例
     */
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 从Spring配置文件（如application.yml/application.properties）中读取数据库连接URL
     * 用于判断当前使用的是哪种类型的数据库（MySQL/SQLite）
     */
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    /**
     * 群组业务服务类，用于初始化数据库后更新默认群组信息
     * @Resource注解：按名称/类型自动注入Spring容器中的Bean
     */
    @Resource
    private GroupService groupService;

    /**
     * 用户业务服务类，用于初始化数据库后创建机器人用户
     */
    @Resource
    private UserService userService;

    /**
     * @PostConstruct注解：Spring Bean的生命周期注解
     * 作用：在Bean的所有属性都被初始化（依赖注入完成）后，自动执行该方法
     * 这是数据库初始化的入口方法
     */
    @PostConstruct
    public void init() {
        // 判断数据库类型
        if (datasourceUrl.startsWith("jdbc:mysql:") || datasourceUrl.startsWith("jdbc:mariadb:")) {
            executeSqlFromFile("meetlink-mysql.sql");
        } else if (datasourceUrl.startsWith("jdbc:postgresql:")) {
            executeSqlFromFile("meetlink-pg.sql");
        } else {
            // 如果不是MySQL，默认按SQLite处理
            // 1. 先创建SQLite数据库文件（如果不存在）
            sqliteCreateDatabase();
            // 2. 执行SQLite专用的SQL脚本文件
            executeSqlFromFile("meetlink-sqlite.sql");
        }
    }

    /**
     * SQLite数据库文件创建方法
     * 作用：根据配置的URL路径，创建SQLite数据库文件（.db文件）
     * SQLite是文件型数据库，需要先创建物理文件才能使用
     */
    public void sqliteCreateDatabase() {
        try {
            String dbFilePath; // 数据库文件的物理路径

            // 从数据库URL中提取文件路径（URL格式：jdbc:sqlite:文件路径?参数）
            if (datasourceUrl.startsWith("jdbc:sqlite:")) {
                // 截取"jdbc:sqlite:"之后的部分，得到文件路径+参数
                dbFilePath = datasourceUrl.substring("jdbc:sqlite:".length());
                // 如果路径包含参数（如?cache=shared），只保留前面的文件路径部分
                if (dbFilePath.contains("?")) {
                    dbFilePath = dbFilePath.substring(0, dbFilePath.indexOf("?"));
                }
            } else {
                // 如果URL不是SQLite格式，抛出非法参数异常
                throw new IllegalArgumentException("Invalid SQLite URL: " + datasourceUrl);
            }

            // 将字符串路径转换为Java NIO的Path对象，方便文件操作
            Path dbPath = Paths.get(dbFilePath);

            // 判断数据库文件是否不存在
            if (Files.notExists(dbPath)) {
                System.out.println("Database file does not exist. Creating database...");
                // 创建空的数据库文件（核心操作）
                Files.createFile(dbPath);
            } else {
                // 文件已存在，跳过创建步骤
                System.out.println("Database file already exists. Skipping initialization.");
            }
            System.out.println("Database initialized successfully.");
        } catch (Exception e) {
            // 捕获所有异常并包装为运行时异常抛出，避免程序静默失败
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * 从指定的SQL文件中读取并执行SQL语句
     * @param resourcePath 资源文件路径（位于项目的resources目录下）
     */
    private void executeSqlFromFile(String resourcePath) {
        // try-with-resources语法：自动关闭流资源，无需手动close()
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                // 从类加载器获取resources目录下的SQL文件输入流
                getClass().getClassLoader().getResourceAsStream(resourcePath),
                StandardCharsets.UTF_8))) { // 指定UTF-8编码，避免中文乱码

            StringBuilder sqlBuilder = new StringBuilder(); // 用于拼接SQL内容
            String line; // 临时存储读取的每一行内容

            // 逐行读取SQL文件内容
            while ((line = reader.readLine()) != null) {
                sqlBuilder.append(line).append("\n"); // 拼接每行内容，保留换行
            }

            // 按分号(;)分割SQL语句，因为一个文件中可能有多个SQL语句
            String[] sqlStatements = sqlBuilder.toString().split(";");

            // 遍历并执行每个SQL语句
            for (String sql : sqlStatements) {
                // 跳过空的SQL语句（比如文件末尾的空行）
                if (!sql.trim().isEmpty()) {
                    // 执行SQL语句（DDL/DML都可以）
                    jdbcTemplate.execute(sql.trim());
                }
            }

            // 数据库初始化完成后，执行业务层的初始化操作
            // 1. 更新默认群组信息
            groupService.updateDefaultGroup();
            // 2. 初始化机器人用户
            userService.initBotUser();

        } catch (Exception e) {
            // 捕获所有异常并包装为运行时异常，向上抛出
            throw new RuntimeException("Failed to execute SQL file: " + resourcePath, e);
        }
    }
}
