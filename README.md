# zhaojunfriend.github.io – Markdown 编辑器

一个功能完整的前后端 Markdown 编辑器，支持实时预览、文件保存/加载。

## 功能特性

- ✅ **实时预览** — 左侧编辑，右侧实时渲染 HTML
- ✅ **语法高亮** — 代码块自动高亮（highlight.js）
- ✅ **格式工具栏** — 加粗、斜体、标题、列表、代码块、引用、表格等一键插入
- ✅ **键盘快捷键** — `Ctrl+S` 保存，`Ctrl+B` 加粗，`Ctrl+I` 斜体
- ✅ **文件管理** — 服务端保存/打开/删除 Markdown 文件（REST API）
- ✅ **可调宽度** — 拖动分隔条调整编辑区与预览区比例
- ✅ **全屏模式** — 隐藏预览，专注写作

## 项目结构

```
├── backend/          # Node.js / Express 后端
│   ├── server.js     # 服务端入口，提供 REST API
│   ├── package.json
│   └── files/        # Markdown 文件存储目录（运行时自动创建）
├── frontend/         # 纯前端（HTML + CSS + JS）
│   ├── index.html
│   ├── style.css
│   ├── app.js
│   ├── marked.min.js       # Markdown 解析器（本地化）
│   ├── highlight.bundle.js # 语法高亮（本地化）
│   └── highlight.css
└── README.md
```

## 快速启动

```bash
# 1. 安装后端依赖
cd backend
npm install

# 2. 启动服务器
npm start
# 或开发模式（热重载）
npm run dev

# 3. 浏览器访问
open http://localhost:3000
```

## REST API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/files` | 列出所有文件 |
| `GET` | `/api/files/:name` | 获取指定文件内容 |
| `POST` | `/api/files` | 保存文件（`{ name, content }`）|
| `DELETE` | `/api/files/:name` | 删除文件 |

## 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `PORT` | `3000` | 服务器监听端口 |
