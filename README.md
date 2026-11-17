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

## 登录示例（Java）

按 Issue #11 确认的默认方案实现：Java + 用户名/密码 + 内存模拟用户表 + Bearer Token 会话 + 受保护示例接口，不包含前端登录页面。

### 环境要求

- JDK 8+
- Maven 3.6+

### 运行

```bash
mvn test                 # 运行单元测试与 HTTP 集成测试
mvn package              # 打包
java -jar target/login-demo-1.0.0.jar
```

默认监听 `127.0.0.1:8080`，可用环境变量覆盖：

```bash
HOST=0.0.0.0 PORT=9090 java -jar target/login-demo-1.0.0.jar
```

演示账号（仅用于演示）：

| 用户名 | 密码 | 显示名 |
| --- | --- | --- |
| demo | demo123 | Demo User |
| admin | admin123 | Administrator |

密码在启动时以 PBKDF2-HMAC-SHA256 哈希保存在内存中，不保存明文。

### 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/login | 用户名密码登录，返回 Bearer Token |
| GET | /api/me | 需要 `Authorization: Bearer <token>`，返回当前用户 |
| POST | /api/logout | 需要 Token，登出后 Token 立即失效（204） |

示例：

```bash
# 登录
curl -s -X POST http://127.0.0.1:8080/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo123"}'
# {"token":"...","tokenType":"Bearer","expiresIn":1800,"username":"demo","displayName":"Demo User"}

# 访问受保护接口
curl -s http://127.0.0.1:8080/api/me \
  -H "Authorization: Bearer $TOKEN"
# {"username":"demo","displayName":"Demo User"}

# 登出
curl -i -X POST http://127.0.0.1:8080/api/logout \
  -H "Authorization: Bearer $TOKEN"
# HTTP/1.1 204 No Content
```

`/api/login` 仅接受 `Content-Type: application/json` 的请求体；`/api/me`、`/api/logout` 需要 Bearer Token。

### 说明与限制

- 用户与会话都保存在内存中，服务重启后重置；未接入数据库（按确认先用内存模拟）。
- Token 默认 30 分钟过期，登出后立即失效；未实现刷新 Token。
- 未包含登录页面/前端，也未实现注册、找回密码。
- 仅用于演示：默认只监听 `127.0.0.1`，未启用 HTTPS、登录限流或失败锁定。
