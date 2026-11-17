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

## 运行

项目使用 Node.js 内置 `http` 模块实现，无第三方依赖，需要 Node.js 18 及以上版本。

```bash
npm start                 # 默认监听 http://127.0.0.1:3000
PORT=8080 npm start       # 指定端口
```

## 接口

### GET /health

健康检查接口，成功时返回 `200` 和如下 JSON：

```json
{"status":"ok"}
```

## 测试

```bash
npm test
```
