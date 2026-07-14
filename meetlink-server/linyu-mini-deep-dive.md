# 林语Mini聊天室 - 深度技术指南

## 📖 前言

本文档旨在为林语Mini聊天室项目提供**一劳永逸的完整技术指南**，深入剖析项目中的核心模块实现、难点技术和架构设计。不同于表面的功能描述，本文将重点讲解：

1. **身份验证与参数解析的完整流程** - 从过滤器到注解的完整链路
2. **缓存机制的设计原理** - Caffeine缓存的应用场景和实现
3. **限流控制的AOP实现** - 注解式限流的设计思想
4. **WebSocket实时通信架构** - Netty与Spring Boot的集成
5. **消息系统的完整处理流程** - 从发送到存储再到推送
6. **AI服务的多路路由机制** - 异步任务与AI API集成
7. **数据库初始化与实体设计** - 双数据库支持的设计

## 🏗️ 项目架构深度解析

### 1. 身份验证系统 - 项目的基石

#### 1.1 完整身份验证流程
```
HTTP请求 → AuthenticationTokenFilter → JWT解析 → 缓存验证 → 设置用户信息 → UserInfoArgumentResolver → 控制器方法
```

#### 1.2 核心组件详解

**1.2.1 JWT Token生成与解析**

**JwtUtil.java** - JWT工具类
```java
@Component
public class JwtUtil implements Serializable {
    private static final long serialVersionUID = -5625635588908941275L;

    // 令牌秘钥（硬编码，生产环境应使用配置）
    private static String secret = "linyu-E7Ymu64s";

    // 令牌有效期：30天
    private static int days = 30;

    /**
     * 创建Token
     * @param claims 包含用户信息的Map
     * @return JWT Token字符串
     */
    public static String createToken(Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expireTime = now.plus(days, ChronoUnit.DAYS);
        return Jwts.builder()
                .setIssuer("cershy")  // 发行者
                .addClaims(claims)    // 自定义声明
                .setExpiration(Date.from(expireTime))  // 过期时间
                .signWith(SignatureAlgorithm.HS256, secret)  // 签名算法
                .compact();
    }

    /**
     * 解析Token
     * @param token JWT Token字符串
     * @return Claims对象，包含所有声明
     */
    public static Claims parseToken(String token) {
        JwtParser jwtParser = Jwts.parser().setSigningKey(secret);
        Jws<Claims> claimsJws = jwtParser.parseClaimsJws(token);
        Claims body = claimsJws.getBody();
        return body;
    }
}
```

**关键点**：
- 使用HS256对称加密算法
- Token有效期为30天
- 包含发行者(issuer)和过期时间(expiration)标准声明
- 支持自定义声明（如userId、userName等）

**1.2.2 身份验证过滤器**

**AuthenticationTokenFilter.java** - 核心过滤器
```java
@Component
@Slf4j
public class AuthenticationTokenFilter extends OncePerRequestFilter {
    private final String TokenName = "x-token";  // Token请求头名称

    @Resource
    private UrlPermitUtil urlPermitUtil;  // URL权限工具

    @Resource
    CacheUtil cacheUtil;  // 缓存工具

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {

        // 跳过OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        String token = request.getHeader(TokenName);
        String url = request.getRequestURI();

        // 检查URL是否需要验证
        if (!urlPermitUtil.isPermitUrl(url)) {
            // 需要验证的URL
            try {
                Claims claims = JwtUtil.parseToken(token);
                // 验证是否在其他地方登录（单点登录）
                String userId = claims.get("userId").toString();
                String cacheToken = cacheUtil.getUserSessionCache(userId);

                if (StrUtil.isBlank(cacheToken)) {
                    // 缓存中没有Token，说明Token已失效
                    tokenInvalid(response, ResultUtil.TokenInvalid().toJSONString(0));
                    return;
                } else if (!cacheToken.equals(token)) {
                    // Token不匹配，说明已在其他地方登录
                    tokenInvalid(response, ResultUtil.LoginElsewhere().toJSONString(0));
                    return;
                }

                // 设置用户信息到请求属性
                setUserInfo(claims, url, request, response);
            } catch (Exception e) {
                // Token解析失败
                tokenInvalid(response, ResultUtil.TokenInvalid().toJSONString(0));
                return;
            }
        } else {
            // 免验证URL，但有Token时也设置用户信息（用于统计等）
            if (StrUtil.isNotBlank(token)) {
                try {
                    Claims claims = JwtUtil.parseToken(token);
                    setUserInfo(claims, url, request, response);
                } catch (Exception e) {
                    // 静默失败，不中断请求
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 设置用户信息到请求属性
     */
    public void setUserInfo(Claims claims, String url,
                           HttpServletRequest request, HttpServletResponse response) {
        // 将Claims转换为Map
        Map<String, Object> map = new HashMap<>();
        claims.entrySet().stream().forEach(e -> map.put(e.getKey(), e.getValue()));

        // 验证角色权限（如果有角色控制）
        String role = (String) map.get("role");
        if (!urlPermitUtil.isRoleUrl(role, url)) {
            tokenInvalid(response, ResultUtil.Forbidden().toJSONString(0));
            return;
        }

        // 设置到请求属性，供后续使用
        request.setAttribute("userinfo", map);
    }
}
```

**关键设计**：
1. **单点登录控制**：通过缓存比对Token，防止多地登录
2. **灵活的URL验证**：支持免验证URL和需要验证URL
3. **角色权限控制**：预留角色权限验证接口
4. **优雅的错误处理**：统一的Token失效响应

**1.2.3 URL权限管理**

**UrlPermitUtil.java** - URL权限工具
```java
@Component
public class UrlPermitUtil {
    // 免验证URL列表
    private List<String> urls = new ArrayList<>();
    // 需要验证角色的URL资源映射
    private Map<String, List<String>> roleUrl = new HashMap<>();

    {
        // 初始免验证URL：WebSocket连接
        urls.add("/ws/**");
    }

    /**
     * 检查URL是否需要验证
     */
    public boolean isPermitUrl(String url) {
        return verifyUrl(url, urls);
    }

    /**
     * 通配符URL匹配算法
     */
    public boolean verifyUrl(String permitUrl, List<String> urlArr) {
        for (String url : urlArr) {
            for (int index = 0; index < url.length(); index++) {
                if (url.charAt(index) == '*') {
                    return true;  // 匹配通配符
                }
                if (permitUrl.length() == index + 1 && url.length() == index + 1) {
                    return true;  // 完全匹配
                }
                if (index == permitUrl.length() || permitUrl.charAt(index) != url.charAt(index)) {
                    break;  // 不匹配
                }
            }
        }
        return false;
    }
}
```

**1.2.4 URL注解扫描器**

**UrlPassRunner.java** - 应用启动时扫描@UrlFree注解
```java
@Component
public class UrlPassRunner implements ApplicationRunner {

    @Resource
    private UrlPermitUtil urlPermitUtil;

    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public void run(ApplicationArguments args) {
        // 获取所有RequestMapping映射
        Map<RequestMappingInfo, HandlerMethod> methodMap = requestMappingHandlerMapping.getHandlerMethods();
        List<String> urlList = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : methodMap.entrySet()) {
            RequestMappingInfo requestMappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            Annotation[] annotations = handlerMethod.getMethod().getAnnotations();

            for (Annotation annotation : annotations) {
                // 扫描@UrlFree注解
                if (annotation.annotationType().equals(UrlFree.class)) {
                    // 获取请求路径，将路径参数替换为通配符
                    Set<String> directPaths = requestMappingInfo.getPatternValues();
                    for (String url : directPaths) {
                        urlList.add(url.replaceAll("\\{[^\\}]+\\}", "**"));
                    }
                }

                // 扫描@UrlResource注解（角色权限）
                if (annotation.annotationType().equals(UrlResource.class)) {
                    UrlResource urlResource = (UrlResource) annotation;
                    String value = urlResource.value();
                    Set<String> directPaths = requestMappingInfo.getPatternValues();
                    for (String url : directPaths) {
                        urlPermitUtil.addRoleUrl(value, url);
                    }
                }
            }
        }

        // 将收集到的免验证URL添加到UrlPermitUtil
        urlPermitUtil.addUrls(urlList);
        logger.info("-----not verify that the url is successfully loaded-----");
    }
}
```

**关键机制**：
1. **启动时扫描**：应用启动时一次性扫描所有控制器方法
2. **动态URL收集**：自动收集带有@UrlFree注解的URL
3. **路径参数处理**：将`{param}`形式的路径参数转换为`**`通配符
4. **角色权限映射**：建立角色与URL的权限映射关系

**1.2.5 自定义参数解析器**

**UserInfoArgumentResolver.java** - 参数解析器
```java
public class UserInfoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 支持@Userid和@UserIp注解的参数
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

**注解定义**：

**Userid.java** - 用户ID注解
```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Userid {
}
```

**UserIp.java** - 用户IP注解
```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UserIp {
}
```

**关键设计**：
1. **类型安全**：通过注解明确参数类型
2. **自动注入**：Spring MVC自动调用解析器
3. **请求上下文**：从Filter设置的属性中获取数据
4. **灵活扩展**：支持添加新的参数注解

**1.2.6 在WebMvcConfig中注册**
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UserInfoArgumentResolver());
    }
}
```

#### 1.3 完整使用示例

**控制器中的使用**：
```java
@RestController
@RequestMapping("/api/v1/message")
public class MessageController {

    @PostMapping("/send")
    public Object send(@Userid String userId,  // 自动注入用户ID
                      @UserIp String userIp,   // 自动注入用户IP
                      @RequestBody SendMessageVo sendMessageVo) {
        sendMessageVo.setUserIp(userIp);
        Message result = messageService.send(userId, sendMessageVo);
        return ResultUtil.Succeed(result);
    }
}
```

**登录控制器中的免验证设置**：
```java
@RestController
@RequestMapping("/api/v1/login")
public class LoginController {

    @UrlFree  // 标记为免验证URL
    @PostMapping("/verify")
    @UrlLimit(keyType = LimitKeyType.IP)  // IP限流
    public Object verify(@RequestBody @Valid VerifyVo verifyVo) {
        String result = loginService.verify(verifyVo.getPassword());
        return ResultUtil.Succeed(result);
    }
}
```

### 2. 缓存机制 - 性能优化的关键

#### 2.1 缓存设计架构
```
用户会话缓存 → 防止多地登录
消息读取缓存 → 优化消息查询性能
限流统计缓存 → 支持高频请求控制
```

#### 2.2 CacheUtil实现详解

**CacheUtil.java** - 缓存管理工具
```java
@Component
public class CacheUtil {
    // 记录用户最后一次查询记录的用户Id（12小时过期）
    private final Cache<String, String> userReadMsgCache;

    // 用户登录信息<用户ID，token>（12小时过期，单点登录控制）
    private final Cache<String, String> userSessionCache;

    public CacheUtil() {
        // 用户阅读消息缓存配置
        this.userReadMsgCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)  // 写入后12小时过期
                .build();

        // 用户会话缓存配置
        this.userSessionCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)  // 写入后12小时过期
                .build();
    }

    /**
     * 存储用户会话Token
     */
    public void putUserSessionCache(String username, String token) {
        userSessionCache.put(username, token);
    }

    /**
     * 获取用户会话Token
     * @return Token或null（表示缓存中没有）
     */
    public String getUserSessionCache(String username) {
        return userSessionCache.getIfPresent(username);
    }

    /**
     * 记录用户最后阅读的消息目标
     */
    public void putUserReadCache(String userId, String targetId) {
        userReadMsgCache.put(userId, targetId);
    }

    /**
     * 获取用户最后阅读的消息目标
     */
    public String getUserReadCache(String userId) {
        return userReadMsgCache.getIfPresent(userId);
    }
}
```

#### 2.3 缓存使用场景分析

**2.3.1 用户会话缓存（单点登录）**
```java
// 登录时存储Token
public JSONObject login(LoginVo loginVo) {
    // ... 用户验证逻辑
    String token = JwtUtil.createToken(userinfo);
    userinfo.put("token", token);

    // 缓存用户会话
    cacheUtil.putUserSessionCache(user.getId(), token);
    return userinfo;
}

// 过滤器验证Token
public void online(Channel channel, String token) {
    Claims claims = JwtUtil.parseToken(token);
    String userId = (String) claims.get("userId");

    // 检查是否已在其他地方登录
    String cacheToken = cacheUtil.getUserSessionCache(userId);
    if (!token.equals(cacheToken)) {
        sendMsg(channel, ResultUtil.Fail("已在其他地方登录"), WsContentType.Msg);
        channel.close();
        return;
    }

    // 记录在线用户
    Online_User.put(userId, channel);
}
```

**2.3.2 消息读取缓存（优化查询）**
```java
public List<Message> record(String userId, RecordVo recordVo) {
    List<Message> messages = messageMapper.record(userId, recordVo.getTargetId(),
            recordVo.getIndex(), recordVo.getNum());

    // 缓存用户最后阅读的目标
    cacheUtil.putUserReadCache(userId, recordVo.getTargetId());
    return messages;
}
```

#### 2.4 Caffeine缓存特性
- **高性能**：基于Window TinyLFU算法，高命中率
- **内存友好**：自动淘汰过期条目
- **线程安全**：并发访问安全
- **监控支持**：可集成监控统计

### 3. 限流控制 - 系统稳定的保障

#### 3.1 限流设计架构
```
@UrlLimit注解 → AOP切面 → Caffeine缓存计数 → 违规统计 → 封禁控制
```

#### 3.2 限流注解定义

**UrlLimit.java** - 限流注解
```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlLimit {
    LimitKeyType keyType() default LimitKeyType.ID;  // 限制类型：用户ID或IP
    int maxRequests() default 60;                     // 每分钟最大请求次数
}
```

**LimitKeyType.java** - 限制键类型枚举
```java
public enum LimitKeyType {
    ID,  // 按用户ID限制
    IP;  // 按IP地址限制
}
```

#### 3.3 AOP限流切面实现

**UrlLimitAspect.java** - 限流切面
```java
@Aspect
@Component
@Slf4j
public class UrlLimitAspect {
    // 请求计数缓存（1分钟过期）
    private final Cache<String, AtomicInteger> requestCountCache;

    // 统计信息缓存（1小时过期，记录违规次数）
    private final Cache<String, UrlLimitStats> statsCache;

    public UrlLimitAspect() {
        this.requestCountCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)  // 1分钟滑动窗口
                .build();

        this.statsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)    // 1小时统计信息
                .build();
    }

    @Pointcut("@annotation(com.cershy.linyuminiserver.annotation.UrlLimit)")
    public void rateLimitPointcut() {
    }

    @Around("rateLimitPointcut() && @annotation(urlLimit)")
    public Object around(ProceedingJoinPoint joinPoint, UrlLimit urlLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        String key = "";
        // 根据限制类型生成Key
        if (urlLimit.keyType() == LimitKeyType.ID) {
            // 从用户信息中获取用户ID
            Map<String, Object> userinfo = (Map<String, Object>) request.getAttribute("userinfo");
            key = userinfo.get("userId").toString();
        } else {
            // 获取客户端IP
            key = IpUtil.getIpAddr(request);
        }

        // 组合Key：标识符 + 请求路径
        String path = request.getRequestURI();
        key = key + ":" + path;

        // 检查是否已被封禁
        UrlLimitStats stats = statsCache.get(key, k -> new UrlLimitStats());
        if (stats.isBlocked()) {
            throw new LinyuException("访问过于频繁，您已被封禁~");
        }

        // 获取并增加计数
        AtomicInteger count = requestCountCache.get(key, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        // 检查是否超过限制
        if (currentCount > urlLimit.maxRequests()) {
            // 记录违规
            stats.setViolationCount(stats.getViolationCount() + 1);
            stats.setLastViolationTime(LocalDateTime.now());

            // 检查是否需要封禁（累计违规超过阈值）
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

**UrlLimitStats.java** - 限流统计信息
```java
@Data
public class UrlLimitStats {
    private int violationCount = 0;      // 违规次数
    private LocalDateTime lastViolationTime;  // 最后一次违规时间
    private boolean blocked = false;     // 是否被封禁
}
```

#### 3.4 限流算法分析

**滑动窗口算法**：
- **窗口大小**：1分钟
- **计数方式**：每个Key独立计数
- **过期策略**：写入后1分钟过期
- **优点**：简单高效，适合API限流

**封禁策略**：
1. **首次超限**：返回"访问过快"提示
2. **累计违规**：记录违规次数
3. **严重违规**：违规超过`maxRequests + 100`次则封禁
4. **封禁时长**：统计缓存1小时过期，自动解封

#### 3.5 使用示例
```java
@RestController
@RequestMapping("/api/v1/message")
public class MessageController {

    @UrlLimit(maxRequests = 100)  // 每分钟最多100次
    @PostMapping("/send")
    public Object send(@Userid String userId,
                      @RequestBody SendMessageVo sendMessageVo) {
        // 业务逻辑
    }

    @UrlLimit(keyType = LimitKeyType.IP)  // 按IP限制
    @GetMapping("/public")
    public Object publicApi() {
        // 公开接口，按IP限流
    }
}
```

### 4. WebSocket实时通信 - Netty深度集成

#### 4.1 WebSocket架构设计
```
Netty服务器(9100端口) → 协议升级处理 → 用户上线管理 → 消息推送 → 心跳检测
```

#### 4.2 Netty服务器配置

**NettyWebSocketServer.java** - Netty服务器
```java
@Slf4j
@Configuration
public class NettyWebSocketServer {

    public static final int Web_Socket_Port = 9100;
    public static final NettyWebSocketServerHandler Netty_Web_Socket_Server_Handler =
            new NettyWebSocketServerHandler();

    // Boss线程组：处理连接请求
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    // Worker线程组：处理IO操作（CPU核心数）
    private EventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors());

    @PostConstruct
    public void start() throws InterruptedException {
        run();
    }

    public void run() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)  // NIO服务器通道
                .option(ChannelOption.SO_BACKLOG, 128)   // 连接队列大小
                .option(ChannelOption.SO_KEEPALIVE, true) // 保持连接
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();

                        // 空闲状态检测（30秒读空闲）
                        pipeline.addLast(new IdleStateHandler(30, 0, 0));

                        // HTTP协议编解码器
                        pipeline.addLast(new HttpServerCodec());

                        // 大数据块写入支持
                        pipeline.addLast(new ChunkedWriteHandler());

                        // HTTP消息聚合器（最大8192字节）
                        pipeline.addLast(new HttpObjectAggregator(8192));

                        // 自定义HTTP头处理器（提取Token）
                        pipeline.addLast(new HttpHeadersHandler());

                        // WebSocket协议处理器（路径/ws）
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

                        // 业务处理器
                        pipeline.addLast(Netty_Web_Socket_Server_Handler);
                    }
                });

        serverBootstrap.bind(Web_Socket_Port).sync();
    }
}
```

#### 4.3 HTTP头处理器（提取Token）

**HttpHeadersHandler.java** - Token提取
```java
public class HttpHeadersHandler extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<String> TOKEN = AttributeKey.valueOf("token");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            // 从请求头中提取Token
            String token = request.headers().get("x-token");
            if (token != null) {
                // 将Token存储到Channel属性中
                ctx.channel().attr(TOKEN).set(token);
            }
        }
        super.channelRead(ctx, msg);
    }
}
```

#### 4.4 WebSocket消息处理器

**NettyWebSocketServerHandler.java** - 核心处理器
```java
@Slf4j
@Sharable
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private WebSocketService webSocketService;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // 延迟初始化，避免循环依赖
        if (this.webSocketService == null) {
            this.webSocketService = SpringUtil.getBean(WebSocketService.class);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 心跳检测：30秒读空闲则断开连接
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                offLine(ctx);
            }
        } else if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            // WebSocket握手完成，用户上线
            String token = NettyUtil.getAttr(ctx.channel(), NettyUtil.TOKEN);
            webSocketService.online(ctx.channel(), token);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 只发送消息，不接受客户端消息（消息通过HTTP API发送）
        // 这种设计分离了控制流和数据流，更清晰
    }
}
```

#### 4.5 在线用户管理

**WebSocketService.java** - 在线用户管理
```java
@Service
public class WebSocketService {

    // 用户ID → Channel映射
    public static final ConcurrentHashMap<String, Channel> Online_User = new ConcurrentHashMap<>();

    // Channel → 用户ID映射
    public static final ConcurrentHashMap<Channel, String> Online_Channel = new ConcurrentHashMap<>();

    public void online(Channel channel, String token) {
        try {
            // 解析Token
            Claims claims = JwtUtil.parseToken(token);
            String userId = (String) claims.get("userId");

            // 单点登录验证
            String cacheToken = cacheUtil.getUserSessionCache(userId);
            if (!token.equals(cacheToken)) {
                sendMsg(channel, ResultUtil.Fail("已在其他地方登录"), WsContentType.Msg);
                channel.close();
                return;
            }

            // 记录在线用户
            Online_User.put(userId, channel);
            Online_Channel.put(channel, userId);

            // 更新用户在线状态
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

    /**
     * 发送消息给指定用户
     */
    public void sendMsgToUser(Object msg, String userId, String targetId) {
        // 发送给发送者
        Channel channel = Online_User.get(userId);
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Msg);
        }

        // 发送给接收者
        channel = Online_User.get(targetId);
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Msg);
        }
    }

    /**
     * 发送消息给所有在线用户（群聊）
     */
    public void sendMsgToGroup(Message message) {
        Online_Channel.forEach((channel, ext) -> {
            sendMsg(channel, message, WsContentType.Msg);
        });
    }

    /**
     * 统一的消息发送方法
     */
    private void sendMsg(Channel channel, Object msg, String type) {
        WsContent wsContent = new WsContent();
        wsContent.setType(type);
        wsContent.setContent(msg);
        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(wsContent)));
    }
}
```

#### 4.6 WebSocket消息格式

**消息结构**：
```json
{
  "type": "msg",  // 消息类型：msg/notify/video/file
  "content": {}   // 实际消息内容
}
```

**消息类型定义**：
```java
public class WsContentType {
    public static final String Msg = "msg";      // 普通消息
    public static final String Notify = "notify"; // 通知
    public static final String Video = "video";   // 视频相关
    public static final String File = "file";     // 文件相关
}
```

### 5. 消息系统 - 核心业务逻辑

#### 5.1 消息处理完整流程
```
HTTP API发送 → 敏感词过滤 → 消息存储 → WebSocket推送 → 聊天列表更新 → AI回复触发
```

#### 5.2 消息实体设计

**Message.java** - 消息实体
```java
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "message", autoResultMap = true)
public class Message {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;                      // UUID主键

    @TableField("from_id")
    private String fromId;                  // 发送方ID

    @TableField("to_id")
    private String toId;                    // 接收方ID

    @TableField(value = "from_info", typeHandler = JacksonTypeHandler.class)
    private UserDto fromInfo;               // 发送方信息（JSON存储）

    @TableField("message")
    private String message;                 // 消息内容（JSON格式）

    @TableField(value = "reference_msg", typeHandler = JacksonTypeHandler.class)
    private Message referenceMsg;           // 引用消息（JSON存储）

    @TableField(value = "at_user", typeHandler = JacksonTypeHandler.class)
    private UserDto userDto;                // @用户信息

    @TableField("is_show_time")
    private Boolean isShowTime;             // 是否显示时间（间隔>5分钟）

    @TableField("type")
    private String type;                    // 消息类型：text/recall/emoji

    @TableField("source")
    private String source;                  // 消息来源：Group/User

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

**关键设计**：
1. **JSON字段存储**：使用MyBatis-Plus的JacksonTypeHandler处理复杂对象
2. **时间显示优化**：间隔超过5分钟的消息显示时间
3. **消息引用**：支持引用回复功能
4. **@功能**：支持@特定用户

#### 5.3 消息发送服务

**MessageServiceImpl.java** - 核心消息服务
```java
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements MessageService {

    @Resource
    SensitiveWordBs sensitiveWordBs;  // 敏感词过滤

    @Resource
    AiChatService aiChatService;      // AI聊天服务

    @Override
    public Message send(String userId, SendMessageVo sendMessageVo) {
        // 路由到群聊或私聊
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

    public Message sendMessage(String userId, SendMessageVo sendMessageVo, String source) {
        // 获取上一条显示时间的消息
        Message previousMessage = messageMapper.getPreviousShowTimeMsg(userId, sendMessageVo.getTargetId());

        // 创建消息对象
        Message message = new Message();
        message.setId(IdUtil.randomUUID());
        message.setFromId(userId);
        message.setSource(source);
        message.setToId(sendMessageVo.getTargetId());

        AtomicReference<UserDto> botUserRef = new AtomicReference<>(null);
        UserDto user = userService.getUserById(userId);

        // 文本消息处理
        if (MessageType.Text.equals(sendMessageVo.getType())) {
            List<TextMessageContent> contents = JSONUtil.toList(
                sendMessageVo.getMsgContent(), TextMessageContent.class);

            contents.forEach(content -> {
                if (TextContentType.Text.equals(content.getType())) {
                    // 敏感词过滤（非机器人用户）
                    if (!UserType.Bot.equals(user.getType())) {
                        content.setContent(sensitiveWordBs.replace(content.getContent()));
                    }
                } else {
                    // @用户处理，检测是否@了机器人
                    UserDto userDto = JSONUtil.toBean(content.getContent(), UserDto.class);
                    if (UserType.Bot.equals(userDto.getType())) {
                        botUserRef.set(userDto);
                    }
                }
            });

            message.setMessage(JSONUtil.toJsonStr(contents));
        } else {
            message.setMessage(sendMessageVo.getMsgContent());
        }

        // 设置消息类型和发送者信息
        message.setType(sendMessageVo.getType());
        user.setIpOwnership(IpUtil.getIpRegion(sendMessageVo.getUserIp()));
        message.setFromInfo(user);

        // 判断是否需要显示时间
        if (null == previousMessage) {
            message.setIsShowTime(true);
        } else {
            message.setIsShowTime(DateUtil.between(
                new Date(), previousMessage.getUpdateTime(), DateUnit.MINUTE) > 5);
        }

        // 保存消息
        if (save(message)) {
            // 触发AI回复（如果@了机器人）
            UserDto botUser = botUserRef.get();
            if (botUser != null) {
                aiChatService.sendBotReply(userId, sendMessageVo.getTargetId(),
                    botUser, extractTextContent(contents));
            }
            return message;
        }
        return null;
    }
}
```

#### 5.4 敏感词过滤集成

**SensitiveWordConfig.java** - 敏感词配置
```java
@Configuration
public class SensitiveWordConfig {

    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        return SensitiveWordBs.newInstance()
                .ignoreCase(true)      // 忽略大小写
                .ignoreWidth(true)     // 忽略半角全角
                .ignoreNumStyle(true)  // 忽略数字样式
                .ignoreChineseStyle(true) // 忽略中文样式
                .ignoreEnglishStyle(true) // 忽略英文样式
                .ignoreRepeat(true)    // 忽略重复词
                .enableNumCheck(true)  // 启用数字检查
                .enableEmailCheck(true) // 启用邮箱检查
                .enableUrlCheck(true)  // 启用URL检查
                .enableWordCheck(true) // 启用单词检查
                .init();               // 初始化敏感词库
    }
}
```

### 6. AI智能聊天 - 多服务集成

#### 6.1 AI服务架构
```
用户@机器人 → AiChatService路由 → 异步调用AI API → 构造回复消息 → WebSocket推送
```

#### 6.2 AI服务路由

**AiChatService.java** - AI服务路由器
```java
@Service
@Slf4j
public class AiChatService {

    @Resource
    DoubaoAiService doubaoAiService;      // 字节豆包

    @Resource
    DeepSeekAiService deepSeekAiService;  // DeepSeek

    @Resource
    RagFlowAiService ragFlowAiService;    // RAGFlow

    @Async("taskExecutor")  // 异步执行，不阻塞主线程
    public void sendBotReply(String userId, String targetId, UserDto botUser, String content) {
        UserDto user = userService.getUserById(userId);

        // 创建@用户的消息内容
        TextMessageContent atUser = new TextMessageContent();
        atUser.setType(TextContentType.At);
        atUser.setContent(JSONUtil.toJsonStr(user, new JSONConfig().setIgnoreNullValue(true)));

        // 根据机器人类型路由
        String reply = "请稍后尝试~";
        switch (botUser.getId()) {
            case "doubao":
                reply = doubaoAiService.ask(userId, content);
                break;
            case "deepseek":
                reply = deepSeekAiService.ask(userId, content);
                break;
            case "ragflow":
                reply = ragFlowAiService.ask(userId, content);
                break;
        }

        // 创建文本回复
        TextMessageContent msgText = new TextMessageContent();
        msgText.setType(TextContentType.Text);
        msgText.setContent(reply);

        // 合并消息（@用户 + 回复）
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

#### 6.3 异步任务配置

**AsyncConfig.java** - 异步配置
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // 核心线程数
        executor.setMaxPoolSize(50);       // 最大线程数
        executor.setQueueCapacity(100);    // 队列容量
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
```

### 7. 数据库设计 - 双数据库支持

#### 7.1 数据库初始化

**DatabaseInitializer.java** - 数据库初始化器
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
            // SQLite需要先创建数据库文件
            sqliteCreateDatabase();
            executeSqlFromFile("linyu-mini-sqlite.sql");
        }
    }

    private void sqliteCreateDatabase() {
        try {
            String dbFilePath = extractDbFilePath(datasourceUrl);
            Path dbPath = Paths.get(dbFilePath);

            if (Files.notExists(dbPath)) {
                Files.createFile(dbPath);  // 创建空的SQLite数据库文件
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void executeSqlFromFile(String resourcePath) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream(resourcePath),
                    StandardCharsets.UTF_8))) {

            StringBuilder sqlBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sqlBuilder.append(line).append("\n");
            }

            // 按分号分割SQL语句
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

#### 7.2 表结构设计

**用户表 (user)**
```sql
CREATE TABLE user (
    id VARCHAR(32) PRIMARY KEY,          -- 用户ID（UUID）
    name VARCHAR(50) NOT NULL,           -- 用户名
    type VARCHAR(20),                    -- 用户类型：User/Bot
    avatar VARCHAR(255),                 -- 头像URL
    email VARCHAR(100),                  -- 邮箱
    badge TEXT,                          -- 徽章列表（JSON数组）
    login_time DATETIME,                 -- 最后登录时间
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name),           -- 用户名唯一
    UNIQUE KEY uk_email (email)          -- 邮箱唯一
);
```

**消息表 (message)**
```sql
CREATE TABLE message (
    id VARCHAR(32) PRIMARY KEY,
    from_id VARCHAR(32) NOT NULL,
    to_id VARCHAR(32) NOT NULL,
    from_info TEXT,                      -- JSON格式的发送者信息
    message TEXT,                        -- JSON格式的消息内容
    reference_msg TEXT,                  -- JSON格式的引用消息
    at_user TEXT,                        -- JSON格式的@用户信息
    is_show_time BOOLEAN DEFAULT FALSE,
    type VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_from_id (from_id),
    INDEX idx_to_id (to_id),
    INDEX idx_create_time (create_time),
    INDEX idx_source (source)
);
```

#### 7.3 MyBatis-Plus配置

**MybatisHandler.java** - 类型处理器配置
```java
@Configuration
public class MybatisHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.setFieldValByName("createTime", new Date(), metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }
}
```

### 8. 配置管理 - 灵活的配置系统

#### 8.1 主配置文件

**application.yml** - 核心配置
```yaml
server:
  port: 9200  # HTTP服务端口

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/linyu?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: 1234

  # Hikari连接池配置
  hikari:
    minimum-idle: 3
    maximum-pool-size: 10
    max-lifetime: 30000

linyu:
  password: sun55@kong          # 群聊密码（RSA加密传输）
  limit: 100                    # 在线人数限制
  name: Linyu在线聊天室         # 群聊名称
  expires: 7                    # 数据过期时间（天）

  # AI服务配置
  doubao:
    api-key: apikey
    count-limit: 5              # 用户使用次数限制
    length-limit: 50            # 回复内容长度限制
    model: ep-20241231132608-lbm7g

  deep-seek:
    api-key: apikey
    count-limit: 2
    length-limit: 50
    model: deepseek-chat

  ragflow:
    host: http://127.0.0.1
    api-key: ragflow-RlNGJiMzNlZjRlZTExZWZhOThkMDI0Mm
    chat-id: 0daeb614f4ed11ef8df20242ac120006
    session-id: f8e2ba0b05f1469c99c34c7369cf230f
    count-limit: 20
    length-limit: 50
```

#### 8.2 配置类绑定

**LinyuConfig.java** - 配置类
```java
@Component
@ConfigurationProperties(prefix = "linyu")
@Data
public class LinyuConfig {
    private String password;      // 群聊密码
    private Integer limit;        // 在线人数限制
    private String name;          // 群聊名称
    private Integer expires;      // 数据过期时间

    private DoubaoConfig doubao;   // 豆包配置
    private DeepSeekConfig deepSeek; // DeepSeek配置
    private RagFlowConfig ragflow; // RAGFlow配置

    @Data
    public static class DoubaoConfig {
        private String apiKey;
        private Integer countLimit;
        private Integer lengthLimit;
        private String model;
    }

    // 其他配置类类似...
}
```

### 9. 安全机制 - 多层次防护

#### 9.1 RSA加密传输

**SecurityUtil.java** - 安全工具
```java
@Slf4j
public final class SecurityUtil {
    private static final KeyPair keyPair;  // RSA密钥对
    private static final BCryptPasswordEncoder passwordEncoder;

    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);  // 1024位密钥
            keyPair = keyGen.generateKeyPair();
            passwordEncoder = new BCryptPasswordEncoder();
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String getPublicKey() {
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        return "-----BEGIN PUBLIC KEY-----\n" +
               Base64.getEncoder().encodeToString(publicKeyBytes) +
               "\n-----END PUBLIC KEY-----";
    }

    public static String decryptPassword(String encryptedPassword) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(
                Base64.getDecoder().decode(encryptedPassword));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new LinyuException("密码解析失败~");
        }
    }
}
```

#### 9.2 登录流程安全
1. **前端**：获取公钥 → RSA加密密码 → 发送加密后的密码
2. **后端**：私钥解密 → 验证密码 → 生成JWT Token
3. **传输安全**：密码加密传输，防止中间人攻击

### 10. 扩展开发指南

#### 10.1 添加新的消息类型
1. 在`MessageType`常量中添加新类型
2. 扩展`Message`实体（如果需要新字段）
3. 实现对应的消息处理器
4. 修改前端消息渲染逻辑

#### 10.2 集成新的AI服务
1. 创建新的AI服务类，实现`ask()`方法
2. 在`AiChatService`中添加路由逻辑
3. 在配置文件中添加API配置
4. 创建对应的机器人用户

#### 10.3 添加新的业务功能
1. 创建对应的`Entity`、`Mapper`、`Service`、`Controller`
2. 设计数据库表结构
3. 实现业务逻辑
4. 添加必要的权限控制

### 11. 性能优化建议

#### 11.1 数据库优化
1. **索引优化**：为常用查询字段添加索引
2. **分表分库**：消息表可按时间分表
3. **查询优化**：避免N+1查询问题

#### 11.2 缓存优化
1. **Redis集成**：替代Caffeine，支持分布式缓存
2. **缓存策略**：热点数据缓存，冷数据淘汰
3. **缓存穿透**：布隆过滤器防止缓存穿透

#### 11.3 网络优化
1. **WebSocket压缩**：启用消息压缩
2. **连接复用**：HTTP/2或HTTP/3支持
3. **CDN加速**：静态资源CDN加速

### 12. 监控与运维

#### 12.1 关键监控指标
1. **在线用户数**：`WebSocketService.getOnlineNum()`
2. **消息发送频率**：限流统计信息
3. **数据库连接池**：Hikari连接池状态
4. **JVM状态**：内存、GC、线程状态

#### 12.2 日志管理
```yaml
logging:
  file:
    name: D:/logs/linyu-mini.log  # 日志文件路径
  level:
    com.cershy.linyuminiserver: DEBUG  # 项目日志级别
```

#### 12.3 健康检查
```java
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public Object health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("onlineUsers", webSocketService.getOnlineNum());
        return health;
    }
}
```

## 🎯 总结

林语Mini聊天室项目展示了现代Java Web应用的全栈技术实践：

### 核心技术亮点
1. **身份验证体系**：JWT + 过滤器 + 参数解析器的完整解决方案
2. **实时通信**：Netty WebSocket高性能实现
3. **限流控制**：基于AOP和Caffeine的灵活限流机制
4. **缓存策略**：Caffeine本地缓存优化性能
5. **AI集成**：多AI服务路由和异步处理
6. **安全机制**：RSA加密传输和敏感词过滤

### 架构设计原则
1. **单一职责**：每个模块职责明确，边界清晰
2. **开闭原则**：通过注解和AOP实现扩展开放
3. **依赖倒置**：面向接口编程，降低耦合
4. **配置化**：所有可变参数外部化配置

### 学习价值
通过深入研究此项目，可以掌握：
- Spring Boot企业级应用架构
- Netty高性能网络编程
- 注解驱动开发模式
- AOP切面编程实践
- 缓存设计和性能优化
- 安全认证和权限控制
- 实时通信系统设计

本项目不仅是一个功能完整的聊天室系统，更是一个优秀的技术学习案例，涵盖了现代Java后端开发的多个重要方面。希望这份深度指南能帮助你更好地理解和掌握项目中的核心技术。