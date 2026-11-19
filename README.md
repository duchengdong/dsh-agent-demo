# dsh-agent-demo
专用于dsh superagent的测试

## 项目介绍

这是一个用于测试 DeepSeek Harness Superagent 功能的演示项目。该项目主要用于验证 AI 代理在代码仓库中的自动化操作能力，包括但不限于：

- 自动读取和理解项目代码
- 根据用户指令修改文件内容
- 创建分支并提交代码更改
- 自动创建和管理 Pull Request
- 与 GitHub Issues 和 PR 进行交互

通过这个演示项目，可以直观地体验 AI 代理如何协助开发者完成日常的代码管理和协作任务，提升开发效率。

## Java Entity 演示

本示例用 JDK 自带的 `HttpServer` 提供固定内容的 HTTP 接口，数据模型是一个 Java entity 类。

- Entity：`src/main/java/com/example/demo/entity/User.java`（`id` / `username` / `displayName` / `enabled`，JavaBean 约定，按主键比较相等）。
- 接口：`GET /api/users/1`，返回由 `User` 实体序列化出的固定 JSON；其他路径返回 `404` JSON。
- 依赖：仅 JDK 8+，无运行时第三方依赖；JUnit 5 仅用于测试。

### 运行

```bash
mvn package
java -jar target/dsh-agent-demo-0.1.0.jar
```

默认监听 `127.0.0.1:8080`，可通过 `HOST` / `PORT` 环境变量覆盖：

```bash
PORT=9090 java -jar target/dsh-agent-demo-0.1.0.jar
curl -i http://127.0.0.1:9090/api/users/1
# HTTP/1.1 200 OK
# Content-Type: application/json; charset=utf-8
# {"id":1,"username":"demo","displayName":"Demo User","enabled":true}
```

### 测试

```bash
mvn test
```

