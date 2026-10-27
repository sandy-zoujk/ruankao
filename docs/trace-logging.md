# traceId 日志关联

仅使用 SLF4J MDC 和 Logback，无 Span、Tracing SDK 或追踪中间件。

## 文件及依赖

代码根目录：`backend/src/main/java/com/sandy/ruankao/`。

- `logging/TraceContext.java`：ID 校验、生成、Runnable/Callable 包装、独立任务作用域。
- `logging/TraceIdFilter.java`：HTTP 请求及 ASYNC/ERROR 分派上下文、响应头、入口日志和 finally 清理。
- `logging/MdcTaskDecorator.java`：任务提交时复制上下文，执行完清理及恢复。
- `logging/TraceIdHttpInterceptor.java`：RestTemplate 出站头覆盖。
- `config/TraceLoggingConfig.java`：Filter 注册、线程池、MVC Callable、RestTemplate。
- `config/WebConfig.java`：CORS 暴露 X-Trace-Id。
- `backend/src/main/resources/logback-spring.xml`：完整控制台与滚动文件配置。

沿用 `org.springframework.boot:spring-boot-starter-web:2.7.18`，它传递引入 `org.springframework.boot:spring-boot-starter-logging:2.7.18`、`ch.qos.logback:logback-classic` 及 `org.slf4j:slf4j-api`，版本由现有 Boot BOM 管理。测试沿用 `org.springframework.boot:spring-boot-starter-test:2.7.18` 和 BOM 管理的 `com.h2database:h2`，均为 test scope。没有新增依赖。Java 8，Servlet 类型为 javax.servlet。

## 数据流和使用

请求 → Filter 读取有效 X-Trace-Id 或生成 32 位 UUID → MDC → 鉴权/Controller/Service 日志 → 响应头 → finally MDC.clear()。
允许的传入值为 1～128 位字母、数字、下划线、连字符；非法值重新生成，不返回 400。
每个异步任务独立复制 MDC，父请求结束不会删除子线程副本。每次 Servlet 分派沿用 request attribute 中的 ID。`request dispatch end` 在 async=true 时仅表示当前分派退出，最终状态看后续 ASYNC/ERROR 分派日志。

业务类直接使用 SLF4J：

```java
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyService.class);
// 方法体内，无需每条消息手动拼接 traceId
log.info("Processing business operation");
```

`@Async` 默认采用 taskExecutor；MVC Callable 采用同一线程池。其他异步任务显式注入 `@Qualifier("taskExecutor") Executor`，再使用 `CompletableFuture.runAsync(task, executor)`。DeferredResult 的生产者也必须使用该执行器。自行管理的 ExecutorService 使用 `submit(TraceContext.wrap(task))` 或 `submit(TraceContext.wrapCallable(callable))`，每次提交重新包装，不要重复缓存包装后的任务。

定时任务每次执行都建立新作用域（当前没有定时任务，不额外开启调度）：

```java
try (TraceContext.Scope ignored = TraceContext.open(null)) {
  log.info("Scheduled execution started");
  // 执行业务，可提交异步任务
}
```

服务间调用注入本项目的 `RestTemplate` Bean；拦截器覆盖旧 X-Trace-Id。没有上下文时为该次调用临时生成 ID，调用后恢复。若多个非 HTTP 操作属于同一业务任务，应在外层 open(null)。Jsoup 公共网页抓取保持原行为。

当前没有 MQ，不引入 MQ 依赖。未来 MQ 适配必须在发送时把 `TraceContext.current()` 写入消息头 X-Trace-Id；消费时 `try (TraceContext.Scope ignored = TraceContext.open(messageHeader)) { ... }`，头缺失则生成。重试/批量消费需要对每条消息分别建立作用域，不能整批共用一个消息的 ID。

文件默认 `logs/application.log`，按日和 20MB 滚动，保留 14 天，总量 1GB。可覆盖 `logging.file.path`、`logging.logback.rollingpolicy.max-file-size`、`max-history`、`total-size-cap`。时间为 Asia/Shanghai。日志包含方法定位信息，有一定调用栈开销；不使用异步 Logback appender，因此无需另行配置 includeCallerData。

## 验证方法

从 backend 目录启动（MySQL 配置沿用项目 README）：

```sh
mvn spring-boot:run
curl -i http://localhost:8080/api/software/info
curl -i -H 'X-Trace-Id: demo-123' http://localhost:8080/api/software/info
curl -i -H 'X-Trace-Id: bad value' http://localhost:8080/api/software/info
curl -i -H 'Origin: http://localhost:5173' -H 'X-Trace-Id: demo-123' http://localhost:8080/api/software/info
curl -i -H 'X-Trace-Id: denied-123' http://localhost:8080/api/admin/users
rg -F 'traceId=demo-123' logs/application.log
```

软件信息查询属于 DEBUG，下面的业务查询行仅在启用项目 DEBUG 后出现。

响应包含 X-Trace-Id；缺失和非法输入均返回新生成的 32 位十六进制值；跨域响应包含 Access-Control-Expose-Headers: X-Trace-Id。日志形态如下（时间、线程、耗时为示意）：

```text
2026-10-27 12:00:00.000 INFO  [traceId=demo-123] [http-nio-8080-exec-1] com.sandy.ruankao.logging.TraceIdFilter.doFilterInternal - request start method=GET path=/api/software/info dispatch=REQUEST
2026-10-27 12:00:00.001 DEBUG [traceId=demo-123] [http-nio-8080-exec-1] com.sandy.ruankao.controller.SoftwareInfoController.info - 查询软件信息
2026-10-27 12:00:00.002 INFO  [traceId=demo-123] [http-nio-8080-exec-1] com.sandy.ruankao.logging.TraceIdFilter.doFilterInternal - request dispatch end method=GET path=/api/software/info status=200 async=false durationMs=2
```

异步业务若记录 `log.info("Async operation")`，日志线程变为 trace-task-1，traceId 仍为 demo-123。项目没有真实异步业务，因此使用测试专用 Controller 验证 MVC Callable，不添加生产调试接口。

```sh
mvn test
mvn -Dtest=TraceLoggingTest,TraceWebIntegrationTest test
```

TraceLoggingTest 使用 2 个工作线程处理 100 个不同 ID，父线程提交后立刻清理，等待全部完成后再裸提交检查工作线程没有残留；还验证任务抛异常、嵌套执行、Callable、独立作用域、ERROR 再分派和模拟 HTTP 头。TraceWebIntegrationTest 验证公开接口、鉴权拦截、CORS、MVC Callable 和请求退出后的 MDC 清理。

## 常见坑

- 异步丢 ID：必须用受管执行器或每次提交时包装；commonPool、parallelStream、第三方回调不会自动传播。
- 线程池串号：任务 finally 清理并恢复原上下文；入口 Filter finally clear。禁止只 put 不清理，禁止 InheritableThreadLocal 替代。
- 网关丢头：网关必须透传请求/响应 X-Trace-Id；后端无法还原被网关丢弃的 ID，只能生成新的。
- 前端读不到头：Filter 提前回写，CORS exposedHeaders 允许读取；反向代理产生的错误响应不经过应用，需代理自身配置。
- 日志没有 traceId：确保使用受管线程，日志经过 SLF4J/Logback；启动日志显示 - 正常。println 和第三方独立日志系统不经过 MDC。
- ID 存在但找不到 SQL：MDC 只关联已记录的日志，不自动开启 SQL 日志。避免为排障直接输出密码、Token、完整请求体及敏感 SQL 参数。
- 单体出站调用：下游也要遵循相同头协议才可关联；本方案不提供父子 Span、耗时拓扑或调用链 UI。


## 业务日志约定

主要 Controller、Service、鉴权拦截器和启动初始化类使用 SLF4J 占位符。Filter 负责 HTTP 生命周期；代理 Service 的 Controller 不重复记录业务操作。学习计划、知识章节、错题直接调用 Mapper，操作日志在其 Controller 记录。

- INFO：注册、登录成功、密码修改、权限变更、练习创建/提交、题库维护和导入、教程同步/发布、学习数据变更及启动初始化。
- DEBUG：查询入口或结果数量、JWT 校验失败等处理分支。
- WARN：登录失败、鉴权/权限拒绝、业务和字段校验失败。全局校验只记录异常类型或错误数量，不直接输出可能包含用户输入的异常消息。
- ERROR：教程同步转换异常处保留异常类型、原调用栈、cause 和 suppressed；日志副本隐藏异常消息，防止 SQL 参数或 URL 查询参数泄露。原异常和接口响应不变。未处理异常继续由 Filter 记录，同样隐藏异常消息，各业务层不重复 catch 后打印。

不记录密码、哈希、Token、密钥、请求体、响应对象、题目答案或文件内容。事务方法的“处理完成”表示方法内处理结束，不表示事务已经提交。日志操作结果不构成独立的审计账本。

默认 INFO。需要排查普通查询时，在 application.yml 中添加：

```yaml
logging:
  level:
    com.sandy.ruankao: DEBUG
```

也可启动时设置 `--logging.level.com.sandy.ruankao=DEBUG`，无需更改 root 级别或滚动配置。

业务日志示意（ID、时间、线程为示例）：

```text
2026-10-27 12:00:00.001 INFO  [traceId=demo-123] [http-nio-8080-exec-1] com.sandy.ruankao.service.PracticeService.create - 练习创建处理完成 userId=7 attemptId=42 questionCount=10
2026-10-27 12:00:00.002 WARN  [traceId=denied-123] [http-nio-8080-exec-2] com.sandy.ruankao.config.AuthInterceptor.preHandle - 鉴权拒绝 reason=会话无效
```

`BusinessLoggingTest` 捕获真实 Logback 事件，验证业务成功/拒绝/校验/同步异常的级别、必要标识及 MDC traceId；检查密码、哈希、Token、URL 查询参数与输入值不出现在捕获的消息和原因链中，并验证普通查询在 INFO 下不输出、DEBUG 下可见。
