# 林语Mini聊天室 - 初学者学习指南

## 📖 项目概述

林语Mini（Linyu-mini）是一款基于 **Spring Boot 2.6.7** 和 **Netty 4.x** 构建的高性能即时通讯在线聊天系统。项目采用轻量化设计，具备快速部署和便捷扩展的特点，适用于企业内部协作、团队沟通以及小型社交平台等多种场景。

### 🎯 核心特性
- **实时消息通讯**：基于WebSocket的即时消息推送
- **多端在线状态**：实时显示用户在线/离线状态
- **群组聊天**：支持多人聊天室功能
- **私聊功能**：用户间一对一私密聊天
- **消息撤回**：2分钟内消息可撤回
- **AI智能聊天**：集成豆包、DeepSeek、RAGFlow等AI服务
- **SSH终端**：内置远程命令执行功能
- **敏感词过滤**：自动过滤敏感内容
- **IP地址定位**：显示用户地理位置
- **限流控制**：防止恶意请求攻击

### 🛠️ 技术栈全景
```
后端框架：Spring Boot 2.6.7 + Java 8
网络通信：Netty 4.1.108.Final
数据持久化：MyBatis-Plus 3.5.3 + MySQL/SQLite
实时通信：WebSocket + Netty
安全认证：JWT + RSA加密
缓存技术：Caffeine本地缓存
工具库：Hutool全能工具库
AI集成：字节豆包、DeepSeek、RAGFlow
SSH服务：Apache SSHD Core
```

## 🏗️ 项目架构设计

### 三层架构模式
```
表现层 (Presentation Layer)
├── Controller：REST API接口
├── WebSocket：实时消息推送
└── SSH Server：远程命令服务

业务层 (Business Layer)
├── Service：业务逻辑实现
├── Manager：复杂业务协调
└── AOP：切面编程（限流、日志等）

数据层 (Data Layer)
├── Entity：数据库实体
├── Mapper：数据访问接口
├── Repository：数据存储
└── Cache：缓存管理
```

### 📁 项目目录结构详解
```
src/main/java/com/cershy/linyuminiserver/
├── annotation/     # 自定义注解
│   ├── CommandInfo.java  # SSH命令注解
│   ├── UrlFree.java      # 免鉴权注解
│   ├── UrlLimit.java     # 限流注解
│   ├── UrlResource.java  # 资源权限注解
│   ├── Userid.java       # 用户ID参数注入
│   └── UserIp.java       # 用户IP参数注入
├── aop/            # 面向切面编程
│   └── UrlLimitAspect.java  # URL限流切面
├── configs/        # Spring配置类
│   ├── AsyncConfig.java      # 异步任务配置
│   ├── DatabaseInitializer.java  # 数据库初始化
│   ├── LinyuConfig.java      # 应用自定义配置
│   ├── MybatisHandler.java   # MyBatis类型处理器
│   ├── SensitiveWordConfig.java  # 敏感词配置
│   ├── UserInfoArgumentResolver.java  # 用户信息参数解析器
│   └── WebMvcConfig.java     # MVC配置（跨域、参数解析器）
├── constant/       # 常量定义
│   ├── BadgeType.java     # 徽章类型常量
│   ├── ChatListType.java  # 聊天列表类型
│   ├── LimitKeyType.java  # 限流键类型
│   ├── MessageSource.java # 消息来源
│   ├── MessageType.java   # 消息类型
│   └── NotifyType.java    # 通知类型
├── controller/     # REST控制器
│   ├── ChatListController.java  # 聊天列表管理
│   ├── FileController.java      # 文件上传下载
│   ├── LoginController.java     # 登录认证
│   ├── MessageController.java   # 消息管理
│   ├── NotifyController.java    # 通知管理
│   ├── UserController.java      # 用户管理
│   └── VideoController.java     # 视频功能
├── dto/           # 数据传输对象
├── entity/        # 数据库实体类
│   ├── ChatList.java  # 聊天列表实体
│   ├── Group.java     # 群组实体
│   ├── Message.java   # 消息实体（核心）
│   ├── Notify.java    # 通知实体
│   └── User.java      # 用户实体
├── exception/     # 自定义异常
├── filter/        # 过滤器
├── mapper/        # MyBatis数据访问层
├── runner/        # 应用启动执行器
├── schedule/      # 定时任务
├── service/       # 业务逻辑层
│   └── impl/      # 业务实现类
├── ssh/           # SSH功能模块
│   └── commands/  # SSH命令实现
├── utils/         # 工具类
│   ├── CacheUtil.java     # 缓存工具
│   ├── IpUtil.java        # IP工具类
│   ├── JwtUtil.java       # JWT工具类
│   ├── ResultUtil.java    # 响应结果工具
│   └── SecurityUtil.java  # 安全工具类
├── vo/            # 视图对象（按模块分类）
│   ├── chatList/  # 聊天列表VO
│   ├── file/      # 文件相关VO
│   ├── login/     # 登录相关VO
│   ├── message/   # 消息相关VO
│   ├── user/      # 用户相关VO
│   └── video/     # 视频相关VO
└── websocket/     # WebSocket实现
    ├── NettyWebSocketServer.java      # Netty服务器
    ├── NettyWebSocketServerHandler.java  # 消息处理器
    └── HttpHeadersHandler.java        # HTTP头处理器
```

## 🔧 核心模块实现详解

### 1. 用户认证系统

#### 1.1 登录流程设计
```
前端请求公钥 → 前端RSA加密密码 → 后端验证密码 → 创建用户/登录 → 生成JWT Token
```

#### 1.2 关键代码实现

**LoginController.java** - 登录控制器
```java
@RestController
@RequestMapping("/api/v1/login")
public class LoginController {

    @Resource
    private LoginService loginService;

    @UrlFree
    @PostMapping("/verify")
    @UrlLimit(keyType = LimitKeyType.IP)
    public Object verify(@RequestBody @Valid VerifyVo verifyVo) {
        String result = loginService.verify(verifyVo.getPassword());
        return ResultUtil.Succeed(result);
    }

    @UrlFree
    @GetMapping("/public-key")
    @UrlLimit(keyType = LimitKeyType.IP)
    public Object getPublicKey() {
        String result = SecurityUtil.getPublicKey();
        return ResultUtil.Succeed(result);
    }

    @UrlFree
    @PostMapping("")
    @UrlLimit(keyType = LimitKeyType.IP)
    public Object login(@RequestBody @Valid LoginVo loginVo) {
        JSONObject result = loginService.login(loginVo);
        return ResultUtil.Succeed(result);
    }
}
```

**LoginService.java** - 登录业务逻辑
```java
@Service
public class LoginService {

    @Value("${linyu.password}")
    private String linyuPassword;

    @Resource
    CacheUtil cacheUtil;

    @Resource
    WebSocketService webSocketService;

    public String verify(String password) {
        // 检查在线人数限制
        if (webSocketService.getOnlineNum() >= linyuLimit) {
            throw new LinyuException("聊天室人数已满，请稍后再试~");
        }
        // RSA解密密码
        String decryptedPassword = SecurityUtil.decryptPassword(password);
        if (!linyuPassword.equals(decryptedPassword)) {
            throw new LinyuException("密码错误~");
        }
        // 生成验证Token
        Map tokenInfo = new HashMap<String, String>();
        tokenInfo.put("type", "verify");
        return JwtUtil.createToken(tokenInfo);
    }

    public JSONObject login(LoginVo loginVo) {
        // 检查在线人数
        if (webSocketService.getOnlineNum() >= linyuLimit) {
            throw new LinyuException("聊天室人数已满，请稍后再试~");
        }

        // 查询或创建用户
        User user = userService.getUserByNameOrEmail(loginVo.getName(), loginVo.getEmail());
        if (user != null) {
            // 用户名/邮箱冲突检查
            if (loginVo.getName().equals(user.getName()) &&
                    !loginVo.getEmail().equals(user.getEmail())) {
                throw new LinyuException("用户名已被使用~");
            }
            // 更新登录时间
            user.setLoginTime(new Date());
            userService.updateById(user);
        } else {
            // 创建新用户
            user = new User();
            user.setId(IdUtil.simpleUUID());
            user.setName(loginVo.getName());
            user.setEmail(loginVo.getEmail());
            user.setLoginTime(new Date());
            user.setType(UserType.User);
            userService.save(user);
        }

        // 生成用户信息和Token
        JSONObject userinfo = new JSONObject();
        userinfo.put("type", "user");
        userinfo.put("userId", user.getId());
        userinfo.put("userName", user.getName());
        userinfo.put("email", user.getEmail());
        userinfo.put("avatar", user.getAvatar());
        String token = JwtUtil.createToken(userinfo);
        userinfo.put("token", token);

        // 缓存用户会话
        cacheUtil.putUserSessionCache(user.getId(), token);
        // 更新用户徽章
        userService.updateUserBadge(user.getId());

        return userinfo;
    }
}
```

#### 1.3 安全机制
- **RSA非对称加密**：前端使用公钥加密密码，后端私钥解密
- **JWT Token认证**：30天有效期的无状态认证
- **会话缓存**：使用Caffeine缓存用户Token，防止多地登录
- **密码验证**：配置文件中配置群聊密码

**SecurityUtil.java** - 安全工具类
```java
public final class SecurityUtil {
    private static final KeyPair keyPair;
    private static final BCryptPasswordEncoder passwordEncoder;

    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            keyPair = keyGen.generateKeyPair();
            passwordEncoder = new BCryptPasswordEncoder();
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String getPublicKey() {
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN PUBLIC KEY-----\n");
        pemBuilder.append(Base64.getEncoder().encodeToString(publicKeyBytes));
        pemBuilder.append("\n-----END PUBLIC KEY-----");
        return pemBuilder.toString();
    }

    public static String decryptPassword(String encryptedPassword) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new LinyuException("密码解析失败~");
        }
    }
}
```

### 2. 实时消息系统

#### 2.1 消息实体设计

**Message.java** - 消息实体类
```java
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "message", autoResultMap = true)
public class Message {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;                      // 消息ID

    @TableField("from_id")
    private String fromId;                  // 发送方ID

    @TableField("to_id")
    private String toId;                    // 接收方ID

    @TableField(value = "from_info", typeHandler = JacksonTypeHandler.class)
    private UserDto fromInfo;               // 发送方信息（JSON存储）

    @TableField("message")
    private String message;                 // 消息内容

    @TableField(value = "reference_msg", typeHandler = JacksonTypeHandler.class)
    private Message referenceMsg;           // 引用消息（JSON存储）

    @TableField(value = "at_user", typeHandler = JacksonTypeHandler.class)
    private UserDto userDto;                // @用户信息

    @TableField("is_show_time")
    private Boolean isShowTime;             // 是否显示时间

    @TableField("type")
    private String type;                    // 消息类型

    @TableField("source")
    private String source;                  // 消息来源

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;                // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;                // 更新时间
}
```

#### 2.2 消息发送流程
```
1. 客户端发送消息 → MessageController.send()
2. 敏感词过滤处理
3. 判断消息类型（群聊/私聊）
4. 保存到数据库
5. WebSocket推送消息
6. 更新聊天列表
```

#### 2.3 消息业务实现

**MessageServiceImpl.java** - 消息服务核心
```java
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Resource
    MessageMapper messageMapper;

    @Resource
    ChatListService chatListService;

    @Resource
    WebSocketService webSocketService;

    @Resource
    SensitiveWordBs sensitiveWordBs;  // 敏感词过滤

    @Resource
    AiChatService aiChatService;      // AI聊天服务

    @Override
    public Message send(String userId, SendMessageVo sendMessageVo) {
        if (MessageSource.Group.equals(sendMessageVo.getSource())) {
            return sendMessageToGroup(userId, sendMessageVo);
        } else {
            return sendMessageToUser(userId, sendMessageVo);
        }
    }

    public Message sendMessageToGroup(String userId, SendMessageVo sendMessageVo) {
        Message message = sendMessage(userId, sendMessageVo, MessageSource.Group);
        // 更新群聊列表
        chatListService.updateChatListGroup(message);
        // WebSocket推送
        webSocketService.sendMsgToGroup(message);
        return message;
    }

    public Message sendMessageToUser(String userId, SendMessageVo sendMessageVo) {
        Message message = sendMessage(userId, sendMessageVo, MessageSource.User);
        // 更新私聊列表
        chatListService.updateChatListPrivate(userId, sendMessageVo.getTargetId(), message);
        // WebSocket推送
        webSocketService.sendMsgToUser(message, userId, sendMessageVo.getTargetId());
        return message;
    }

    public Message sendMessage(String userId, SendMessageVo sendMessageVo, String source) {
        // 获取上一条显示时间的消息（用于时间间隔判断）
        Message previousMessage = messageMapper.getPreviousShowTimeMsg(userId, sendMessageVo.getTargetId());

        // 创建消息对象
        Message message = new Message();
        message.setId(IdUtil.randomUUID());
        message.setFromId(userId);
        message.setSource(source);
        message.setToId(sendMessageVo.getTargetId());

        StringBuffer sb = new StringBuffer();
        AtomicReference<UserDto> botUserRef = new AtomicReference<>(null);
        UserDto user = userService.getUserById(userId);

        // 文本消息处理（敏感词过滤）
        if (MessageType.Text.equals(sendMessageVo.getType())) {
            List<TextMessageContent> contents = JSONUtil.toList(sendMessageVo.getMsgContent(), TextMessageContent.class);
            contents.forEach(content -> {
                if (TextContentType.Text.equals(content.getType())) {
                    // 非机器人用户进行敏感词过滤
                    if (!UserType.Bot.equals(user.getType())) {
                        content.setContent(sensitiveWordBs.replace(content.getContent()));
                    }
                    sb.append(content.getContent());
                } else {
                    // @用户处理，检测是否@了机器人
                    UserDto userDto = JSONUtil.toBean(content.getContent(), UserDto.class);
                    if (UserType.Bot.equals(userDto.getType())) {
                        botUserRef.set(JSONUtil.toBean(content.getContent(), UserDto.class));
                    }
                }
            });
            message.setMessage(JSONUtil.toJsonStr(contents));
        } else {
            message.setMessage(sendMessageVo.getMsgContent());
        }

        message.setType(sendMessageVo.getType());
        user.setIpOwnership(IpUtil.getIpRegion(sendMessageVo.getUserIp()));
        message.setFromInfo(user);

        // 判断是否需要显示时间（间隔超过5分钟）
        if (null == previousMessage) {
            message.setIsShowTime(true);
        } else {
            message.setIsShowTime(DateUtil.between(new Date(), previousMessage.getUpdateTime(), DateUnit.MINUTE) > 5);
        }

        // 引用消息处理
        if (StrUtil.isNotBlank(sendMessageVo.getReferenceMsgId())) {
            Message referenceMessage = getById(sendMessageVo.getReferenceMsgId());
            referenceMessage.setReferenceMsg(null);
            message.setReferenceMsg(referenceMessage);
        }

        // 保存消息
        if (save(message)) {
            // 如果@了机器人，触发AI回复
            UserDto botUser = botUserRef.get();
            if (botUser != null) {
                aiChatService.sendBotReply(userId, sendMessageVo.getTargetId(), botUser, sb.toString());
            }
            return message;
        }
        return null;
    }

    @Override
    public Message recall(String userId, RecallVo recallVo) {
        Message message = getById(recallVo.getMsgId());
        if (null == message) {
            throw new LinyuException("消息不存在~");
        }
        if (!message.getFromId().equals(userId)) {
            throw new LinyuException("仅能撤回自己的消息~");
        }

        // 限制撤回时间（2分钟内）
        if (DateUtil.between(message.getCreateTime(), new Date(), DateUnit.MINUTE) > 2) {
            throw new LinyuException("消息已超过2分钟，无法撤回~");
        }

        // 更新消息为撤回状态
        message.setType(MessageType.Recall);
        message.setMessage("");
        updateById(message);

        // 推送撤回通知
        if (MessageSource.Group.equals(message.getSource())) {
            chatListService.updateChatListGroup(message);
            webSocketService.sendMsgToGroup(message);
        } else {
            chatListService.updateChatListPrivate(userId, message.getToId(), message);
            webSocketService.sendMsgToUser(message, userId, message.getToId());
        }
        return message;
    }
}
```

### 3. WebSocket实时通信

#### 3.1 Netty服务器配置

**NettyWebSocketServer.java** - Netty服务器
```java
@Slf4j
@Configuration
public class NettyWebSocketServer {

    public static final int Web_Socket_Port = 9100;
    public static final NettyWebSocketServerHandler Netty_Web_Socket_Server_Handler = new NettyWebSocketServerHandler();

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors());

    @PostConstruct
    public void start() throws InterruptedException {
        run();
    }

    public void run() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new IdleStateHandler(30, 0, 0));  // 30秒读空闲检测
                        pipeline.addLast(new HttpServerCodec());           // HTTP编解码
                        pipeline.addLast(new ChunkedWriteHandler());       // 大数据块写入
                        pipeline.addLast(new HttpObjectAggregator(8192));  // HTTP消息聚合
                        pipeline.addLast(new HttpHeadersHandler());        // 自定义HTTP头处理
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));  // WebSocket协议
                        pipeline.addLast(Netty_Web_Socket_Server_Handler); // 业务处理器
                    }
                });
        serverBootstrap.bind(Web_Socket_Port).sync();
    }
}
```

#### 3.2 WebSocket消息处理器

**NettyWebSocketServerHandler.java** - 消息处理
```java
@Slf4j
@Sharable
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private WebSocketService webSocketService;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (this.webSocketService == null) {
            this.webSocketService = SpringUtil.getBean(WebSocketService.class);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            // 读空闲，关闭连接（心跳检测）
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                offLine(ctx);
            }
        } else if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            // 握手完成，获取Token并上线
            String token = NettyUtil.getAttr(ctx.channel(), NettyUtil.TOKEN);
            webSocketService.online(ctx.channel(), token);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 只发送消息，不接受客户端消息（消息通过HTTP API发送）
    }
}
```

#### 3.3 WebSocket服务管理

**WebSocketService.java** - 在线用户管理
```java
@Service
public class WebSocketService {

    public static final ConcurrentHashMap<String, Channel> Online_User = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Channel, String> Online_Channel = new ConcurrentHashMap<>();

    public void online(Channel channel, String token) {
        try {
            // 解析JWT Token
            Claims claims = JwtUtil.parseToken(token);
            String userId = (String) claims.get("userId");

            // 检查是否已在其他地方登录（单点登录）
            String cacheToken = cacheUtil.getUserSessionCache(userId);
            if (!token.equals(cacheToken)) {
                sendMsg(channel, ResultUtil.Fail("已在其他地方登录"), WsContentType.Msg);
                channel.close();
                return;
            }

            // 记录在线用户
            Online_User.put(userId, channel);
            Online_Channel.put(channel, userId);
            userService.online(userId);
        } catch (Exception e) {
            sendMsg(channel, ResultUtil.Fail("连接错误"), WsContentType.Msg);
            channel.close();
        }
    }

    public void offline(Channel channel) {
        String userId = Online_Channel.get(channel);
        if (StrUtil.isNotBlank(userId)) {
            Online_User.remove(userId);
            Online_Channel.remove(channel);
            userService.offline(userId);
        }
    }

    // 发送消息给指定用户
    public void sendMsgToUser(Object msg, String userId, String targetId) {
        Channel channel = Online_User.get(userId);
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Msg);
        }
        channel = Online_User.get(targetId);
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Msg);
        }
    }

    // 发送消息给所有在线用户（群聊）
    public void sendMsgToGroup(Message message) {
        Online_Channel.forEach((channel, ext) -> {
            sendMsg(channel, message, WsContentType.Msg);
        });
    }
}
```

### 4. 限流控制机制

#### 4.1 限流注解设计

**UrlLimit.java** - 限流注解
```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlLimit {
    LimitKeyType keyType() default LimitKeyType.ID;  // 限制类型：IP或用户ID
    int maxRequests() default 60;                     // 每分钟最大请求次数
}
```

#### 4.2 AOP限流实现

**UrlLimitAspect.java** - 限流切面
```java
@Aspect
@Component
@Slf4j
public class UrlLimitAspect {
    private final Cache<String, AtomicInteger> requestCountCache;
    private final Cache<String, UrlLimitStats> statsCache;

    public UrlLimitAspect() {
        // 请求计数缓存（1分钟过期）
        this.requestCountCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();
        // 统计信息缓存（1小时过期）
        this.statsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    @Around("rateLimitPointcut() && @annotation(urlLimit)")
    public Object around(ProceedingJoinPoint joinPoint, UrlLimit urlLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        String key = "";
        // 根据类型生成Key：用户ID或IP地址
        if (urlLimit.keyType() == LimitKeyType.ID) {
            Map<String, Object> userinfo = (Map<String, Object>) request.getAttribute("userinfo");
            key = userinfo.get("userId").toString();
        } else {
            key = IpUtil.getIpAddr(request);
        }

        String path = request.getRequestURI();
        key = key + ":" + path;

        // 检查是否被封禁
        UrlLimitStats stats = statsCache.get(key, k -> new UrlLimitStats());
        if (stats.isBlocked()) {
            throw new LinyuException("访问过于频繁，您已被封禁~");
        }

        // 获取并增加计数
        AtomicInteger count = requestCountCache.get(key, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        if (currentCount > urlLimit.maxRequests()) {
            // 记录违规
            stats.setViolationCount(stats.getViolationCount() + 1);
            stats.setLastViolationTime(LocalDateTime.now());

            // 检查是否需要封禁（违规次数超过阈值）
            if (stats.getViolationCount() >= urlLimit.maxRequests() + 100) {
                stats.setBlocked(true);
                throw new LinyuException("访问过于频繁，您已被封禁~");
            }

            statsCache.put(key, stats);
            throw new LinyuException("访问过快，请稍后再试~");
        }

        return joinPoint.proceed();
    }
}
```

### 5. AI智能聊天集成

#### 5.1 AI服务架构
```
用户@机器人 → AiChatService → 路由到对应AI服务 → 获取回复 → 发送消息
```

#### 5.2 AI聊天服务

**AiChatService.java** - AI服务路由
```java
@Service
@Slf4j
public class AiChatService {

    @Resource
    DoubaoAiService doubaoAiService;      // 字节豆包服务
    @Resource
    DeepSeekAiService deepSeekAiService;  // DeepSeek服务
    @Resource
    RagFlowAiService ragFlowAiService;    // RAGFlow服务

    @Async("taskExecutor")
    public void sendBotReply(String userId, String targetId, UserDto botUser, String content) {
        UserDto user = userService.getUserById(userId);

        // 创建@用户的消息内容
        TextMessageContent atUser = new TextMessageContent();
        atUser.setType(TextContentType.At);
        JSONConfig config = new JSONConfig().setIgnoreNullValue(true);
        atUser.setContent(JSONUtil.toJsonStr(user, config));

        // 根据机器人类型路由到不同的AI服务
        String ask = "请稍后尝试~";
        switch (botUser.getId()) {
            case "doubao":
                ask = doubaoAiService.ask(userId, content);
                break;
            case "deepseek":
                ask = deepSeekAiService.ask(userId, content);
                break;
            case "ragflow":
                ask = ragFlowAiService.ask(userId, content);
                break;
        }

        // 创建文本回复消息
        TextMessageContent msgText = new TextMessageContent();
        msgText.setType(TextContentType.Text);
        msgText.setContent(ask);

        // 合并消息内容（@用户 + 回复内容）
        JSONArray msgContent = new JSONArray();
        msgContent.add(atUser);
        msgContent.add(msgText);

        // 发送消息
        SendMessageVo sendMessageVo = new SendMessageVo();
        sendMessageVo.setTargetId(targetId);
        sendMessageVo.setSource(MessageSource.Group);
        sendMessageVo.setMsgContent(msgContent.toJSONString(0));
        sendMessageVo.setUserIp("机器人");
        sendMessageVo.setType(MessageType.Text);

        messageService.sendMessageToGroup(botUser.getId(), sendMessageVo);
    }
}
```

### 6. 数据库设计与初始化

#### 6.1 数据库表结构

**用户表 (user)**
```sql
CREATE TABLE user (
    id VARCHAR(32) PRIMARY KEY,          -- 用户ID
    name VARCHAR(50) NOT NULL,           -- 用户名
    type VARCHAR(20),                    -- 用户类型
    avatar VARCHAR(255),                 -- 头像URL
    email VARCHAR(100),                  -- 邮箱
    badge TEXT,                          -- 徽章列表（JSON）
    login_time DATETIME,                 -- 最后登录时间
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**消息表 (message)**
```sql
CREATE TABLE message (
    id VARCHAR(32) PRIMARY KEY,          -- 消息ID
    from_id VARCHAR(32) NOT NULL,        -- 发送方ID
    to_id VARCHAR(32) NOT NULL,          -- 接收方ID
    from_info TEXT,                      -- 发送方信息（JSON）
    message TEXT,                        -- 消息内容
    reference_msg TEXT,                  -- 引用消息（JSON）
    at_user TEXT,                        -- @用户信息（JSON）
    is_show_time BOOLEAN DEFAULT FALSE,  -- 是否显示时间
    type VARCHAR(20) NOT NULL,           -- 消息类型
    source VARCHAR(20) NOT NULL,         -- 消息来源
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_from_id (from_id),
    INDEX idx_to_id (to_id),
    INDEX idx_create_time (create_time)
);
```

#### 6.2 数据库初始化

**DatabaseInitializer.java** - 数据库初始化
```java
@Component
public class DatabaseInitializer {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        // 判断数据库类型
        if (datasourceUrl.startsWith("jdbc:mysql:")) {
            executeSqlFromFile("linyu-mini-mysql.sql");
        } else {
            // SQLite数据库
            sqliteCreateDatabase();
            executeSqlFromFile("linyu-mini-sqlite.sql");
        }
    }

    private void executeSqlFromFile(String resourcePath) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resourcePath),
                StandardCharsets.UTF_8))) {

            StringBuilder sqlBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sqlBuilder.append(line).append("\n");
            }

            // 按分号分割并执行SQL语句
            String[] sqlStatements = sqlBuilder.toString().split(";");
            for (String sql : sqlStatements) {
                if (!sql.trim().isEmpty()) {
                    jdbcTemplate.execute(sql.trim());
                }
            }

            // 执行业务初始化
            groupService.updateDefaultGroup();
            userService.initBotUser();

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL file: " + resourcePath, e);
        }
    }
}
```

### 7. 缓存管理

#### 7.1 缓存设计

**CacheUtil.java** - 缓存工具类
```java
@Component
public class CacheUtil {
    // 记录用户最后一次查询记录的用户Id
    private final Cache<String, String> userReadMsgCache;
    // 用户登录信息<用户名，token>
    private final Cache<String, String> userSessionCache;

    public CacheUtil() {
        this.userReadMsgCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
        this.userSessionCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    public void putUserSessionCache(String username, String token) {
        userSessionCache.put(username, token);
    }

    public String getUserSessionCache(String username) {
        return userSessionCache.getIfPresent(username);
    }

    public void putUserReadCache(String userId, String targetId) {
        userReadMsgCache.put(userId, targetId);
    }

    public String getUserReadCache(String userId) {
        return userReadMsgCache.getIfPresent(userId);
    }
}
```

### 8. 配置管理

#### 8.1 应用配置文件

**application.yml** - 主配置文件
```yaml
server:
  port: 9200

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/linyu?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: 1234

linyu:
  password: sun55@kong          # 群聊密码
  limit: 100                    # 在线人数限制
  name: Linyu在线聊天室         # 群聊名称
  expires: 7                    # 数据过期时间（天）

  doubao:                       # 豆包AI配置
    api-key: apikey
    count-limit: 5              # 次数限制
    length-limit: 50            # 内容长度限制
    model: ep-20241231132608-lbm7g

  deep-seek:                    # DeepSeek AI配置
    api-key: apikey
    count-limit: 2
    length-limit: 50
    model: deepseek-chat

  ragflow:                      # RAGFlow配置
    host: http://127.0.0.1
    api-key: ragflow-RlNGJiMzNlZjRlZTExZWZhOThkMDI0Mm
    chat-id: 0daeb614f4ed11ef8df20242ac120006
    session-id: f8e2ba0b05f1469c99c34c7369cf230f
    count-limit: 20
    length-limit: 50
```

#### 8.2 WebMVC配置

**WebMvcConfig.java** - MVC配置
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 注册自定义参数解析器
        resolvers.add(new UserInfoArgumentResolver());
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        // 跨域配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }
}
```

#### 8.3 自定义参数解析器

**UserInfoArgumentResolver.java** - 用户信息参数解析
```java
public class UserInfoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Userid.class) ||
                parameter.hasParameterAnnotation(UserIp.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        if (parameter.hasParameterAnnotation(Userid.class)) {
            // 从请求属性中获取用户ID
            Map<String, Object> userinfo = (Map<String, Object>) request.getAttribute("userinfo");
            if (userinfo != null) {
                return userinfo.get("userId");
            }
        }

        if (parameter.hasParameterAnnotation(UserIp.class)) {
            // 获取客户端IP地址
            String ipAddr = IpUtil.getIpAddr(request);
            if (ipAddr != null) {
                return ipAddr;
            }
        }

        return null;
    }
}
```

## 🚀 部署与运行

### 环境要求
- Java 8+
- MySQL 5.7+ 或 SQLite
- Maven 3.6+

### 启动步骤
1. **数据库配置**
   ```bash
   # 创建MySQL数据库
   CREATE DATABASE linyu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **修改配置文件**
   ```bash
   # 编辑 src/main/resources/application.yml
   # 修改数据库连接信息
   # 配置群聊密码和其他参数
   ```

3. **编译运行**
   ```bash
   # 使用Maven编译
   mvn clean package

   # 运行项目
   java -jar target/linyu-mini-server-0.0.1-SNAPSHOT.jar
   ```

4. **访问应用**
   ```
   WebSocket服务: ws://localhost:9100/ws
   HTTP API服务: http://localhost:9200
   ```

### Docker部署
```bash
# 构建Docker镜像
docker build -t linyu-mini .

# 运行容器
docker run -d -p 9200:9200 -p 9100:9100 \
  -v /path/to/config:/config \
  --name linyu-mini linyu-mini
```

## 📚 学习路径建议

### 第一阶段：基础理解（1-2天）
1. **项目结构熟悉**
   - 理解Maven项目结构
   - 掌握Spring Boot启动流程
   - 了解三层架构划分

2. **核心配置文件**
   - 学习application.yml配置
   - 理解Spring Bean配置
   - 掌握数据库连接配置

### 第二阶段：核心模块学习（3-5天）
1. **用户认证系统**
   - RSA加密原理
   - JWT Token实现
   - 会话管理机制

2. **消息系统**
   - 消息实体设计
   - 敏感词过滤实现
   - 消息撤回逻辑

3. **WebSocket通信**
   - Netty服务器搭建
   - 在线用户管理
   - 心跳检测机制

### 第三阶段：高级特性（5-7天）
1. **AOP限流控制**
   - 注解式编程
   - Caffeine缓存使用
   - 限流算法实现

2. **AI集成**
   - 多AI服务路由
   - 异步任务处理
   - API调用封装

3. **SSH功能**
   - Apache SSHD集成
   - 命令模式设计
   - 安全权限控制

### 第四阶段：扩展开发（7-10天）
1. **新功能添加**
   - 添加新的消息类型
   - 实现文件分享功能
   - 开发新的AI机器人

2. **性能优化**
   - 数据库查询优化
   - 缓存策略优化
   - 并发处理优化

3. **安全加固**
   - XSS防护
   - SQL注入防护
   - DDoS防护增强

## 🔍 调试与故障排除

### 常见问题
1. **数据库连接失败**
   ```
   检查：application.yml中的数据库配置
   验证：MySQL服务是否运行
   测试：使用客户端连接数据库
   ```

2. **WebSocket连接失败**
   ```
   检查：Netty服务器端口(9100)是否被占用
   验证：防火墙设置
   测试：使用WebSocket测试工具连接
   ```

3. **JWT认证失败**
   ```
   检查：Token生成和解析逻辑
   验证：Token有效期设置
   测试：使用在线JWT工具调试
   ```

### 日志查看
```bash
# 查看应用日志
tail -f D:/logs/linyu-mini.log

# 查看SQL日志
# 在application.yml中开启MyBatis日志
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

## 🎯 实战练习

### 练习1：添加新消息类型
**目标**：实现图片消息功能
**步骤**：
1. 在MessageType常量中添加`Image`类型
2. 扩展Message实体，添加图片URL字段
3. 实现图片上传接口
4. 修改消息发送逻辑，支持图片消息
5. 前端适配图片消息显示

### 练习2：实现消息已读功能
**目标**：显示消息已读状态
**步骤**：
1. 在Message实体中添加已读状态字段
2. 创建消息已读记录表
3. 实现消息已读标记接口
4. WebSocket推送已读状态
5. 前端显示已读/未读状态

### 练习3：添加新的AI机器人
**目标**：集成ChatGPT或文心一言
**步骤**：
1. 创建新的AI服务类（如ChatGPTAiService）
2. 在配置文件中添加API配置
3. 在AiChatService中添加路由逻辑
4. 创建对应的机器人用户
5. 测试AI对话功能

## 📖 学习资源推荐

### 官方文档
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Netty官方文档](https://netty.io/wiki/)
- [MyBatis-Plus文档](https://baomidou.com/)

### 技术博客
- Spring Boot实战系列
- Netty高性能网络编程
- WebSocket实时通信原理
- JWT认证最佳实践

### 视频教程
- Spring Boot从入门到精通
- Netty网络编程实战
- 即时通讯系统开发
- 微服务架构设计

## 🤝 贡献指南

### 代码规范
1. **命名规范**
   - 类名使用大驼峰：`UserService`
   - 方法名使用小驼峰：`getUserById`
   - 常量使用全大写：`MAX_REQUEST`

2. **注释要求**
   - 类注释：说明类的作用
   - 方法注释：说明参数、返回值、异常
   - 复杂逻辑：添加行内注释

3. **提交规范**
   - 提交信息格式：`feat: 添加新功能` 或 `fix: 修复问题`
   - 一次提交一个功能/修复
   - 提交前运行测试

### 开发流程
1. Fork项目到个人仓库
2. 创建功能分支：`git checkout -b feature/xxx`
3. 开发测试
4. 提交代码：`git commit -m "feat: xxx"`
5. 推送分支：`git push origin feature/xxx`
6. 创建Pull Request

## 📄 许可证

本项目采用开源许可证，具体信息请查看LICENSE文件。

---

## 🎉 总结

林语Mini聊天室是一个功能完整、架构清晰的即时通讯项目，涵盖了现代Web应用开发的多个重要方面：

1. **架构设计**：清晰的三层架构，模块化设计
2. **技术栈**：Spring Boot + Netty + MyBatis-Plus主流技术组合
3. **功能完整**：用户系统、消息系统、实时通信、AI集成等
4. **代码质量**：规范的代码结构，良好的注释
5. **扩展性**：易于添加新功能和模块

通过学习和实践这个项目，你可以掌握：
- Spring Boot企业级应用开发
- Netty高性能网络编程
- WebSocket实时通信
- JWT安全认证
- AOP切面编程
- 缓存技术应用
- 数据库设计与优化

希望这个项目能成为你学习Java后端开发的良好起点，祝学习愉快！