# 管理后台(演示)

Vue 3 + TypeScript + Vite + Element Plus + ECharts。

这是后端项目的演示管理端,包含 4 个页面:

| 页面 | 功能 | 后端接口 |
|---|---|---|
| 数据看板 | KPI + 近 7 天订单/GMV 趋势 | `GET /api/v1/admin/dashboard/summary` |
| 商户审核 | 列表、通过(选店铺分类)、驳回 | `/api/v1/admin/merchants` |
| 内容审核 | 笔记列表、隐藏 | `/api/v1/admin/blog-posts` |
| 知识库 | 导入/删除 AI 知识文档 | `/api/v1/admin/ai/knowledge` |

## 启动

```bash
# 1. 安装依赖
npm install

# 2. 启动后端(默认 8080)
#    如果后端在 8084:
VITE_API_TARGET=http://localhost:8084 npm run dev

# 3. 打开
# http://localhost:5173
```

登录需要 ADMIN 账号。演示数据里的管理员是 `13900000000`。

开发环境验证码会打印在后端日志里:

```text
[SMS] phone=13900000000 code=123456
```

## 构建

```bash
npm run build
```

产物在 `dist/`。部署时把 `dist/` 放到 Nginx,并把 `/api` 反向代理到后端:

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
}

location / {
    root /path/to/clp-admin-web/dist;
    try_files $uri $uri/ /index.html;
}
```

> 路由使用 hash 模式(`#/dashboard`),静态服务器不需要额外配置 history fallback。

## 目录

```text
src/
├── api/
│   ├── http.ts        axios 实例、token 注入、统一错误处理
│   ├── auth.ts        登录 / 验证码
│   ├── admin.ts       管理端接口
│   └── types.ts       后端响应类型
├── layouts/
│   └── AdminLayout.vue
├── router/
│   └── index.ts       路由与登录守卫
├── views/
│   ├── LoginView.vue
│   ├── DashboardView.vue
│   ├── MerchantAuditView.vue
│   ├── BlogModerationView.vue
│   └── KnowledgeView.vue
├── App.vue
├── main.ts
└── styles/global.css
```

## 前端知识点

- **SFC**:一个 `.vue` 文件 = 模板 + 逻辑 + 样式。
- **Composition API**:`ref` / `computed` / `onMounted`,状态和逻辑按功能组织。
- **Vue Router**:hash 路由 + `beforeEach` 登录守卫。
- **Axios 拦截器**:请求自动带 `Authorization`,响应统一解包 `Result.data`,2001/2003 自动回登录页。
- **Vite proxy**:开发时 `/api` 转发到后端,避免 CORS。
- **Element Plus**:表格、分页、弹窗、表单、消息提示。
- **ECharts**:看板的柱状图和折线图。
