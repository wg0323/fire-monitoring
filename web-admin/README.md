# 消防监控平台后台（web-admin）

Vue 3 + TypeScript + Vite 5 + Element Plus 的轻量自建后台壳，覆盖登录、应用壳、大屏、火警处置、处置跟踪、工单、电子档案、商户/设备/巡检/外呼配置、统计分析、系统与字典页面。

## 环境要求

- Node.js 18+，npm 9+。
- 后端 `fm-api` 运行在 `http://127.0.0.1:8080`（默认代理目标）。

## 快速开始

```bash
npm install
npm run dev        # http://localhost:5174
npm run typecheck  # vue-tsc --noEmit
npm run build      # 产出 dist/
npm run check      # typecheck + build，交付前门禁
```

如需代理到其他后端端口（cmd 用 `set`；PowerShell 用 `$env:`）：

```bash
# cmd
set VITE_API_TARGET=http://127.0.0.1:18080
npm run dev
```

```powershell
# PowerShell
$env:VITE_API_TARGET = "http://127.0.0.1:18080"
npm run dev
```

## 前端密钥与轮换

高德地图 Web 端凭据放在 `.env.local`（已被 `.gitignore` 忽略，切勿提交、切勿写入文档或日志）。变量名见 `.env.example`：`VITE_AMAP_KEY`（JS API Key）与 `VITE_AMAP_SECURITY_CODE`（安全密钥）。二者会随前端产物下发，属于可公开但需受控的凭据：

- 定期在高德开放平台控制台轮换，先新增 Key/安全密钥并配置域名白名单，再更新本地 `.env.local` 与各部署环境的同名变量；
- 更新后重新执行 `npm run build` 并发布前端，确认线上生效后再停用旧 Key；
- 疑似泄露时立即轮换并缩小 Referer / 域名白名单，不要在 IM、截图、工单里粘贴真实值。

## 演示账号（fm_p1 + p1seed）

| 账号 | 角色 | 密码 |
| --- | --- | --- |
| `admin` | 平台管理员 | `123456` |
| `cd_admin` | 昌都租户管理员 | `123456` |
| `cd_duty` | 昌都租户值守 | `123456` |

外勤与商户账号只允许登录小程序，后台登录会被拒绝。

## 目录

```text
web-admin/src/
├── api/          # 后台接口封装
├── components/   # FmStatusTag、FmMenuIcon 等轻量组件
├── directives/   # v-auth 按钮权限
├── layouts/      # 侧栏、顶栏、页签、应用壳
├── router/       # 静态路由 + 菜单守卫 + 占位页
├── stores/       # auth / app / dict
├── styles/       # --fm-* 令牌与 Element Plus 覆盖
├── types/        # API 类型
├── utils/        # request、格式化、菜单树
└── views/        # 登录、大屏、报警、跟踪、工单、档案、服务、统计、系统、字典
```

## 说明

- 侧栏由后端菜单树驱动；月度报告、年度报告页本期显示“建设中”，其余业务页面已接入。
- 平台管理员对租户业务只读，前端隐藏写按钮，后端写接口统一 403；前端对 403 统一提示并刷新权限信息，避免权限变更后长期不生效。
- 接口文档使用 Knife4j/OpenAPI3，后端启动后访问 `http://localhost:8080/doc.html`；接口契约以 `docs/新版产品设计.md` 为准。
- 外呼配置页支持七陌账号；无话机联调时后端设置 `FM_CALL_MOCK=true`，页面会显示模拟模式。
