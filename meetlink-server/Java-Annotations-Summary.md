# Java 注解分类总结

本文档对项目中使用的 Java 注解进行分类总结，帮助初学者理解各注解的作用和使用场景。

## 1. Spring 核心注解

### @SpringBootApplication
- **原始作用**：开启 Spring Boot 自动配置、组件扫描和 Spring 配置功能，是 Spring Boot 应用的入口注解。
- **项目中作用**：作为启动类 `LinyuMiniServerApplication` 的注解，启动整个 Spring Boot 项目。
- **可配置参数**：
  - **scanBasePackages**：组件扫描的基础包，默认为空（扫描声明类所在的包及其子包）
  - **scanBasePackageClasses**：组件扫描的基础包对应的类，用于类型安全的包指定
  - **exclude**：要排除的自动配置类
  - **excludeName**：要排除的自动配置类名
  - **proxyBeanMethods**：是否代理 `@Bean` 方法以强制执行 Bean 生命周期行为，默认为 true
- **同类型注解举例**：`@Configuration`、`@ComponentScan`、`@EnableAutoConfiguration`（这三个注解的组合等价于 `@SpringBootApplication`）。

### @Configuration
- **原始作用**：标记一个类为配置类，替代 XML 配置文件，该类中可以声明 `@Bean` 方法。
- **项目中作用**：用于配置类（如 `AsyncConfig`、`WebMvcConfig`），定义 Bean 或配置组件。
- **同类型注解举例**：`@SpringBootApplication`（内部包含 `@Configuration`）、`@Component`。

### @Bean
- **原始作用**：在 `@Configuration` 类的方法上使用，将方法返回的对象注册为 Spring 容器中的 Bean。
- **项目中作用**：在配置类中定义 Bean，例如 `AsyncConfig` 中定义 `taskExecutor` 线程池，`WebMvcConfig` 中定义 `corsFilter` 跨域过滤器。
- **可配置参数**：
  - **value**：Bean 的名称，默认为空（使用方法名）
  - **name**：Bean 的名称，可以是字符串数组，指定多个名称
  - **autowire**：自动装配模式，默认为 `Autowire.NO`（不自动装配）
  - **initMethod**：初始化方法名称
  - **destroyMethod**：销毁方法名称
- **同类型注解举例**：`@Component`、`@Service`、`@Repository`（用于类级别，而 `@Bean` 用于方法级别）。

### @Component
- **原始作用**：通用的 Spring 组件注解，标记一个类为 Spring 容器管理的 Bean。
- **项目中作用**：用于普通组件类，如 `ExpiredClearTask`（定时任务）、`UrlLimitAspect`（AOP 切面）。
- **同类型注解举例**：`@Service`、`@Repository`、`@Controller`（都是 `@Component` 的特化）。

### @Service
- **原始作用**：标记一个类为业务逻辑层的服务组件，是 `@Component` 的特化。
- **项目中作用**：用于服务实现类，如 `UserServiceImpl`、`AiChatService`，表示业务逻辑层。
- **同类型注解举例**：`@Component`、`@Repository`、`@Controller`。

### @MapperScan
- **原始作用**：指定 MyBatis 或 MyBatis-Plus 的 Mapper 接口的扫描路径。
- **项目中作用**：在启动类上扫描 `com.cershy.linyuminiserver.mapper` 包，将 Mapper 接口注册为 Spring Bean。
- **可配置参数**：
  - **value**：要扫描的包路径，可以是字符串数组
  - **basePackages**：要扫描的包路径，与 value 作用相同
  - **basePackageClasses**：要扫描的包对应的类，用于类型安全的包指定
  - **nameGenerator**：Bean 名称生成器
  - **sqlSessionTemplateRef**：SqlSessionTemplate Bean 名称
  - **sqlSessionFactoryRef**：SqlSessionFactory Bean 名称
- **同类型注解举例**：`@ComponentScan`（用于扫描普通组件）、`@EntityScan`（用于 JPA 实体扫描）。

## 2. Spring MVC 注解

### @RestController
- **原始作用**：组合 `@Controller` 和 `@ResponseBody`，表示该类是 RESTful 控制器，返回值直接写入 HTTP 响应体。
- **项目中作用**：用于控制器类，如 `UserController`、`LoginController`，处理 HTTP 请求并返回 JSON 数据。
- **同类型注解举例**：`@Controller`（需配合 `@ResponseBody`）、`@RequestMapping`（可定义类级别路径）。

### @RequestMapping
- **原始作用**：映射 HTTP 请求到控制器方法，可指定路径、请求方法等。
- **项目中作用**：用于控制器类级别（如 `@RequestMapping("/api/v1/user")`）定义基础路径。
- **可配置参数**：
  - **value**：请求路径，可以是字符串或字符串数组
  - **method**：HTTP 请求方法，如 `RequestMethod.GET`、`RequestMethod.POST` 等
  - **params**：请求参数条件，如 `"name=test"` 表示必须有 name=test 参数
  - **headers**：请求头条件，如 `"Content-Type=application/json"`
  - **consumes**：请求内容类型，如 `"application/json"`
  - **produces**：响应内容类型，如 `"application/json"`
- **同类型注解举例**：`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`（是 `@RequestMapping` 的快捷方式）。

### @GetMapping
- **原始作用**：`@RequestMapping(method = RequestMethod.GET)` 的简写，映射 GET 请求。
- **项目中作用**：用于控制器方法，如 `UserController` 中的 `listUser()`、`onlineWeb()` 等方法。
- **可配置参数**：
  - **value**：请求路径，可以是字符串或字符串数组
  - **params**：请求参数条件，如 `"name=test"` 表示必须有 name=test 参数
  - **headers**：请求头条件，如 `"Content-Type=application/json"`
  - **consumes**：请求内容类型，如 `"application/json"`
  - **produces**：响应内容类型，如 `"application/json"`
- **同类型注解举例**：`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@RequestMapping`。

### @PostMapping
- **原始作用**：`@RequestMapping(method = RequestMethod.POST)` 的简写，映射 POST 请求。
- **项目中作用**：用于控制器方法，如 `UserController` 中的 `updateUser()`、`LoginController` 中的 `verify()` 等方法。
- **可配置参数**：
  - **value**：请求路径，可以是字符串或字符串数组
  - **params**：请求参数条件，如 `"name=test"` 表示必须有 name=test 参数
  - **headers**：请求头条件，如 `"Content-Type=application/json"`
  - **consumes**：请求内容类型，如 `"application/json"`
  - **produces**：响应内容类型，如 `"application/json"`
- **同类型注解举例**：`@GetMapping`、`@PutMapping`、`@DeleteMapping`、`@RequestMapping`。

### @RequestBody
- **原始作用**：将 HTTP 请求体中的 JSON/XML 数据绑定到方法的参数对象。
- **项目中作用**：用于控制器方法的参数，如 `createUser(@RequestBody @Valid CreateUserVo createUserVo)`，接收前端传递的 JSON 数据。
- **可配置参数**：
  - **required**：是否必须提供请求体，默认为 true
  - **contentType**：请求内容类型，默认为空
- **同类型注解举例**：`@RequestParam`（绑定查询参数）、`@PathVariable`（绑定路径变量）。

### @RequestParam
- **原始作用**：将 HTTP 请求参数绑定到方法参数。
- **项目中作用**：项目中未显式使用，但 Spring MVC 支持该注解。
- **可配置参数**：
  - **value**：请求参数名称，默认为空（使用参数名）
  - **required**：是否必须提供该参数，默认为 true
  - **defaultValue**：默认值，当请求中未提供该参数时使用
- **同类型注解举例**：`@RequestBody`、`@PathVariable`、`@RequestHeader`。

### @PathVariable
- **原始作用**：将 URL 路径中的变量绑定到方法参数。
- **项目中作用**：项目中未显式使用，但 Spring MVC 支持该注解。
- **可配置参数**：
  - **value**：路径变量名称，默认为空（使用参数名）
  - **required**：是否必须提供该路径变量，默认为 true
  - **name**：路径变量名称，与 value 作用相同
- **同类型注解举例**：`@RequestParam`、`@RequestBody`。

## 3. 依赖注入注解

### @Resource
- **原始作用**：JSR-250 标准注解，用于依赖注入，默认按名称（name）匹配 Bean，其次按类型。
- **项目中作用**：广泛用于注入依赖，如 `UserController` 中注入 `UserService`，`UserServiceImpl` 中注入 `UserMapper`、`WebSocketService` 等。
- **可配置参数**：
  - **name**：Bean 的名称，按名称注入时使用
  - **type**：Bean 的类型，按类型注入时使用
  - **lookup**：查找名称，用于 JNDI 查找
  - **mappedName**：映射名称，用于特定应用服务器的映射
- **同类型注解举例**：`@Autowired`（Spring 专有，默认按类型匹配）、`@Inject`（JSR-330 标准注解）。

### @Autowired
- **原始作用**：Spring 专有注解，用于依赖注入，默认按类型匹配 Bean。
- **项目中作用**：项目中未显式使用，但 Spring 容器支持该注解。
- **可配置参数**：
  - **value**：指定要注入的 Bean 名称，默认为空（按类型匹配）
  - **required**：是否必须注入，默认为 true，设为 false 时找不到 Bean 不会报错
- **同类型注解举例**：`@Resource`、`@Inject`。

### @Value
- **原始作用**：注入外部配置（如 `application.properties`）的值到字段、方法或参数。
- **项目中作用**：用于 `ExpiredClearTask` 中注入 `linyu.expires` 配置值，表示过期天数。
- **可配置参数**：
  - **value**：配置属性键名或 SpEL 表达式，如 `"${linyu.expires}"` 或 `"#{systemProperties['user.dir']}"`
- **同类型注解举例**：`@ConfigurationProperties`（批量注入配置属性）。

### @Lazy
- **原始作用**：延迟初始化 Bean，直到第一次被使用时才创建。
- **项目中作用**：在 `AiChatService` 中用于 `UserService` 和 `MessageService` 字段，避免循环依赖。
- **可配置参数**：
  - **value**：是否延迟初始化，默认为 true（延迟）
- **同类型注解举例**：`@DependsOn`（指定 Bean 初始化顺序）。

## 4. MyBatis-Plus 注解

### @TableName
- **原始作用**：指定实体类对应的数据库表名。
- **项目中作用**：用于实体类 `User`，指定表名为 `user`，并启用自动结果映射（`autoResultMap = true`）。
- **可配置参数**：
  - **value**：数据库表名，默认为空（使用实体类名）
  - **schema**：数据库 schema，默认为空
  - **resultMap**：XML 中 resultMap 的 id，默认为空
  - **autoResultMap**：是否自动构建 resultMap 并使用，默认为 false
- **同类型注解举例**：`@Table`（JPA 注解）、`@Entity`（JPA 实体注解）。

### @TableId
- **原始作用**：标记实体类的主键字段，可指定主键类型和生成策略。
- **项目中作用**：用于 `User` 实体类的 `id` 字段，标记为主键，默认对应数据库列 `id`。
- **可配置参数**：
  - **value**：数据库列名，默认为空（使用字段名）
  - **type**：主键类型，默认为 `IdType.NONE`（无主键策略），可选 `AUTO`（数据库自增）、`INPUT`（手动输入）、`ASSIGN_ID`（分配ID）、`ASSIGN_UUID`（分配UUID）等
- **同类型注解举例**：`@Id`（JPA 主键注解）、`@GeneratedValue`（JPA 主键生成策略）。

### @TableField
- **原始作用**：标记实体类的非主键字段，可指定数据库列名、是否存在、填充策略等。
- **项目中作用**：用于 `User` 实体类的各个字段，如 `name`、`type`、`avatar` 等，指定列名和填充策略（如 `create_time` 插入时填充，`update_time` 插入和更新时填充）。
- **可配置参数**：
  - **value**：数据库列名，默认为空（使用字段名）
  - **exist**：字段是否在数据库中存在，默认为 true，设为 false 表示非数据库字段
  - **fill**：字段自动填充策略，默认为 `FieldFill.DEFAULT`（不填充），可选 `INSERT`（插入时填充）、`UPDATE`（更新时填充）、`INSERT_UPDATE`（插入和更新时填充）
  - **typeHandler**：类型处理器，用于处理复杂类型的转换，如 `JacksonTypeHandler` 用于 JSON 字段
  - **select**：是否在查询时默认选中该字段，默认为 true
  - **condition**：查询条件，默认为空
- **同类型注解举例**：`@Column`（JPA 列注解）、`@Transient`（标记非持久化字段）。

## 5. MyBatis 注解

### @Select
- **原始作用**：在 Mapper 接口方法上声明 SQL 查询语句。
- **项目中作用**：用于 `UserMapper` 中的方法，如 `getUserById`、`listUser`、`listMapUser`，直接编写 SQL 查询。
- **可配置参数**：
  - **value**：SQL 查询语句，可以是字符串数组，多个语句会合并执行
- **同类型注解举例**：`@Insert`、`@Update`、`@Delete`、`@Results`（结果映射）。

### @ResultMap
- **原始作用**：引用 XML 映射文件中定义的 `resultMap`，用于结果映射。
- **项目中作用**：在 `UserMapper` 的查询方法上引用 `UserDtoResultMap`（可能在 XML 中定义），将查询结果映射到 `UserDto` 对象。
- **可配置参数**：
  - **value**：XML 中 resultMap 的 id，可以是字符串数组引用多个 resultMap
- **同类型注解举例**：`@Results`（内联结果映射）、`@Result`（单个字段映射）。

### @MapKey
- **原始作用**：指定返回 Map 时，将结果集的哪个字段作为 Map 的 key。
- **项目中作用**：用于 `UserMapper` 的 `listMapUser` 方法，指定将 `id` 字段作为 Map 的 key。
- **可配置参数**：
  - **value**：结果集中作为 Map key 的字段名
- **同类型注解举例**：`@ResultMap`、`@Result`。

## 6. Lombok 注解

### @Data
- **原始作用**：组合注解，包含 `@Getter`、`@Setter`、`@ToString`、`@EqualsAndHashCode`、`@RequiredArgsConstructor`，为类生成 getter、setter、toString、equals 和 hashCode 方法。
- **项目中作用**：用于实体类 `User`、VO 类 `CreateUserVo`、`UpdateUserVo` 等，简化代码。
- **同类型注解举例**：`@Getter`、`@Setter`、`@ToString`、`@EqualsAndHashCode`（可单独使用）。

### @EqualsAndHashCode
- **原始作用**：生成 `equals()` 和 `hashCode()` 方法。
- **项目中作用**：在 `User` 实体类中与 `@Data` 一起使用，`callSuper = false` 表示不调用父类的方法。
- **可配置参数**：
  - **callSuper**：是否调用父类的 `equals()` 和 `hashCode()` 方法，默认为 false
  - **exclude**：排除的字段列表
  - **of**：包含的字段列表
  - **doNotUseGetters**：是否不使用 getter 方法访问字段，默认为 false
- **同类型注解举例**：`@Data`（已包含）、`@ToString`。

### @Accessors
- **原始作用**：配置 Lombok 生成的 getter/setter 的链式调用风格。
- **项目中作用**：在 `User` 实体类中设置 `chain = true`，支持链式调用（如 `user.setName("abc").setEmail("...")`）。
- **可配置参数**：
  - **chain**：是否启用链式调用，默认为 false
  - **fluent**：是否启用流式调用（方法名不带 get/set 前缀），默认为 false
  - **prefix**：字段名前缀，自动去除前缀生成 getter/setter 方法名
- **同类型注解举例**：`@Data`、`@Setter`（可单独设置链式调用）。

### @Slf4j
- **原始作用**：为类生成一个 `log` 字段，用于日志记录（使用 SLF4J API）。
- **项目中作用**：用于服务类、AOP 切面等需要记录日志的类，如 `UserServiceImpl`、`UrlLimitAspect`。
- **同类型注解举例**：`@Log4j`、`@Log4j2`、`@CommonsLog`（不同日志框架的注解）。

## 7. 验证注解

### @Valid
- **原始作用**：触发 JSR-303/JSR-380 Bean Validation，对方法参数进行校验。
- **项目中作用**：用于控制器方法的参数，如 `createUser(@RequestBody @Valid CreateUserVo createUserVo)`，触发 `CreateUserVo` 中的校验规则。
- **可配置参数**：
  - **groups**：校验分组，用于在不同场景下应用不同的校验规则
  - **payload**：负载信息，可用于传递元数据
- **同类型注解举例**：`@Validated`（Spring 的增强版本，支持分组校验）。

### @NotBlank
- **原始作用**：校验字符串不能为 null 且至少包含一个非空白字符。
- **项目中作用**：用于 VO 类的字段，如 `CreateUserVo` 的 `name` 和 `email` 字段，确保用户输入不为空。
- **可配置参数**：
  - **message**：校验失败时的错误消息，如 `"用户名不能为空~"`
  - **groups**：校验分组，用于在不同场景下应用不同的校验规则
  - **payload**：负载信息，可用于传递元数据
- **同类型注解举例**：`@NotNull`（不能为 null，但可以为空字符串）、`@NotEmpty`（不能为 null 且长度/大小大于 0）、`@Size`（长度范围）。

## 8. 异步/定时注解

### @EnableAsync
- **原始作用**：启用 Spring 的异步方法执行功能，允许在方法上使用 `@Async`。
- **项目中作用**：在启动类上启用异步支持，使得 `AiChatService` 中的 `@Async` 方法生效。
- **可配置参数**：
  - **proxyTargetClass**：是否使用 CGLIB 代理，默认为 false（使用 JDK 动态代理）
  - **mode**：代理模式，可选 `AdviceMode.PROXY`（代理模式）或 `AdviceMode.ASPECTJ`（AspectJ 模式）
  - **order**：执行顺序，默认为 `Ordered.LOWEST_PRECEDENCE`
- **同类型注解举例**：`@EnableScheduling`（启用定时任务）、`@EnableCaching`（启用缓存）。

### @EnableScheduling
- **原始作用**：启用 Spring 的定时任务功能，允许在方法上使用 `@Scheduled`。
- **项目中作用**：在启动类上启用定时任务支持，使得 `ExpiredClearTask` 中的 `@Scheduled` 方法生效。
- **可配置参数**：
  - **proxyTargetClass**：是否使用 CGLIB 代理，默认为 false（使用 JDK 动态代理）
  - **mode**：代理模式，可选 `AdviceMode.PROXY`（代理模式）或 `AdviceMode.ASPECTJ`（AspectJ 模式）
  - **order**：执行顺序，默认为 `Ordered.LOWEST_PRECEDENCE`
- **同类型注解举例**：`@EnableAsync`、`@EnableCaching`。

### @Async
- **原始作用**：标记一个方法为异步执行，调用时将在一个单独的线程中运行。
- **项目中作用**：用于 `AiChatService` 的 `sendBotReply` 方法，指定使用 `taskExecutor` 线程池执行，避免阻塞主线程。
- **可配置参数**：
  - **value**：指定使用的线程池 Bean 名称，默认为空（使用默认线程池）
- **同类型注解举例**：`@Scheduled`（定时执行）、`@EventListener`（事件监听）。

### @Scheduled
- **原始作用**：标记一个方法为定时任务，可指定 cron 表达式、固定延迟等。
- **项目中作用**：用于 `ExpiredClearTask` 的 `deleteExpiredContent` 方法，每天凌晨执行（cron = "0 0 0 * * ?"），清理过期数据。
- **可配置参数**：
  - **cron**：Cron 表达式，定义任务执行时间规则（如 `"0 0 0 * * ?"` 表示每天凌晨执行）
  - **fixedRate**：固定速率执行，单位毫秒，从上一次任务开始时间计算下一次执行时间
  - **fixedDelay**：固定延迟执行，单位毫秒，从上一次任务完成时间计算下一次执行时间
  - **initialDelay**：初始延迟时间，单位毫秒，应用启动后延迟多久开始第一次执行
  - **zone**：时区，用于解析 cron 表达式
- **同类型注解举例**：`@Async`、`@EventListener`。

## 9. AOP 注解

### @Aspect
- **原始作用**：声明一个类为切面，包含切入点和通知。
- **项目中作用**：用于 `UrlLimitAspect` 类，定义 URL 限流切面。
- **同类型注解举例**：`@Component`（切面也需要被 Spring 管理）。

### @Component
- **原始作用**：将切面类注册为 Spring Bean。
- **项目中作用**：在 `UrlLimitAspect` 上使用，使切面被 Spring 容器管理。
- **同类型注解举例**：`@Aspect`、`@Service`、`@Repository`。

### @Pointcut
- **原始作用**：定义切入点表达式，指定哪些方法需要被增强。
- **项目中作用**：在 `UrlLimitAspect` 中定义 `rateLimitPointcut`，切入所有被 `@UrlLimit` 注解的方法。
- **可配置参数**：
  - **value**：切入点表达式，如 `"@annotation(com.cershy.linyuminiserver.annotation.UrlLimit)"`
- **同类型注解举例**：`@Around`、`@Before`、`@After`（通知注解可以引用切入点）。

### @Around
- **原始作用**：环绕通知，在目标方法执行前后进行增强，可以控制是否执行目标方法。
- **项目中作用**：用于 `UrlLimitAspect` 的 `around` 方法，实现限流逻辑，根据请求频率决定是否继续执行。
- **可配置参数**：
  - **value**：切入点表达式，指定要增强的方法，如 `"rateLimitPointcut() && @annotation(urlLimit)"`
- **同类型注解举例**：`@Before`、`@After`、`@AfterReturning`、`@AfterThrowing`。

## 10. 自定义注解

### @UrlLimit
- **原始作用**：自定义注解，用于标记需要限流的方法，可指定限流类型和最大请求数。
- **项目中作用**：用于控制器方法（如 `UserController` 的 `listUser`、`LoginController` 的 `verify`），配合 `UrlLimitAspect` 实现基于 IP 或用户 ID 的限流。
- **可配置参数**：
  - **keyType**：限流键类型，默认为 `LimitKeyType.ID`（按用户ID限流），可选 `LimitKeyType.IP`（按IP地址限流）
  - **maxRequests**：每分钟最大请求次数，默认为 60 次
- **同类型注解举例**：`@UrlFree`、`@UrlResource`（项目中其他自定义注解）。

### @Userid
- **原始作用**：自定义注解，用于标记控制器方法参数，表示该参数应从用户信息中提取用户 ID。
- **项目中作用**：用于 `UserController` 的 `updateUser` 方法的 `userid` 参数，通过 `UserInfoArgumentResolver` 解析注入。
- **同类型注解举例**：`@RequestParam`、`@PathVariable`（Spring 内置参数注解）。

### @UrlFree
- **原始作用**：自定义注解，用于标记不需要限流的方法或类。
- **项目中作用**：用于 `LoginController` 的 `verify`、`getPublicKey`、`login` 方法，表示这些接口不受限流限制。
- **可配置参数**：
  - **value**：可选值，可用于传递额外信息，默认为空字符串
- **同类型注解举例**：`@UrlLimit`、`@UrlResource`。

### @UrlResource
- **原始作用**：自定义注解，用于标记需要资源控制的方法或类。
- **项目中作用**：项目中未具体使用，但已定义。
- **可配置参数**：
  - **value**：可选值，可用于传递额外信息，默认为空字符串
- **同类型注解举例**：`@UrlLimit`、`@UrlFree`。

### @CommandInfo
- **原始作用**：自定义注解，用于标记 SSH 命令类，提供命令名称和描述。
- **项目中作用**：用于 SSH 命令类（如 `LinyuHelpCommand`），定义命令的元信息。
- **可配置参数**：
  - **description**：命令描述信息，说明命令的功能
  - **name**：命令名称，在 SSH 中使用的命令名
- **同类型注解举例**：无（项目特定注解）。

## 11. 元注解（Meta-Annotations）

元注解是用于定义其他注解的注解，项目中自定义注解使用了以下元注解：

### @Target
- **原始作用**：指定注解可以应用的目标元素类型（如类、方法、字段等）。
- **项目中作用**：用于所有自定义注解（如 `@UrlLimit`、`@Userid`），定义注解的使用范围。
- **可配置参数**：
  - **value**：目标元素类型数组，如 `ElementType.METHOD`、`ElementType.TYPE`、`ElementType.PARAMETER` 等
- **同类型注解举例**：`@Retention`、`@Documented`、`@Inherited`。

### @Retention
- **原始作用**：指定注解的保留策略（源码、类文件、运行时）。
- **项目中作用**：用于所有自定义注解，设置为 `RetentionPolicy.RUNTIME`，以便在运行时通过反射读取。
- **可配置参数**：
  - **value**：保留策略，可选 `RetentionPolicy.SOURCE`（源码）、`RetentionPolicy.CLASS`（类文件）、`RetentionPolicy.RUNTIME`（运行时）
- **同类型注解举例**：`@Target`、`@Documented`、`@Inherited`。

### @Documented
- **原始作用**：指示该注解应包含在 Javadoc 中。
- **项目中作用**：用于部分自定义注解（如 `@UrlFree`、`@UrlResource`、`@Userid`），使注解信息出现在文档中。
- **同类型注解举例**：`@Target`、`@Retention`、`@Inherited`。

## 总结

本项目中使用的注解涵盖了 Spring Boot、MyBatis-Plus、Lombok、验证、异步定时、AOP 等多个方面，同时也有自定义注解用于扩展功能。通过注解，项目实现了配置简化、依赖注入、数据校验、限流控制、定时任务等常见功能，体现了 Spring Boot 框架的便捷性和扩展性。

建议初学者结合代码实际使用场景，理解每个注解的作用，并尝试在类似项目中应用。