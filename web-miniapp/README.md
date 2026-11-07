# 消防监控平台小程序（web-miniapp）

微信原生小程序 + TypeScript + Vant Weapp，单 appid 多角色：租户管理员、租户外勤、商户共用同一套页面，由角色切换 tabBar 与功能入口。

## 环境要求

- 微信开发者工具稳定版，基础库 3.4.10 及以上。
- Node.js 18+，npm 9+。
- AppID：`project.config.json` 需填写已认证小程序 AppID；开发者工具的游客模式（`touristappid`）仅能在模拟器里预览，无法使用微信手机号授权、合法域名与上传发布。`project.config.json` / `project.private.config.json` 的 `urlCheck: false` 短期保留，用于本地 HTTP 调试。

## 快速开始

1. 在 `web-miniapp` 目录执行 `npm install`。
2. 用微信开发者工具导入 `web-miniapp` 根目录（`project.config.json` 已配置 `miniprogramRoot`）。
3. 在 `project.config.json` 中替换 `appid`。
4. 工具栏选择「工具 → 构建 npm」，生成 `miniprogram/miniprogram_npm`（Vant Weapp）。
5. 开发版默认连 `http://127.0.0.1:8080/api/mp`（`DEV_API_BASE`）；体验版/正式版需先填写 `RELEASE_API_BASE`，再编译预览。

## 配置

| 配置 | 位置 | 说明 |
| --- | --- | --- |
| API 地址 | `miniprogram/config/index.ts` | `develop` 用 `DEV_API_BASE`（`http://127.0.0.1:8080/api/mp`）；`trial`/`release` 用 `RELEASE_API_BASE` |
| 小程序 AppID | `project.config.json` | `appid` 字段 |
| 开发手机号登录 | 后端 `FM_WX_MOCK_LOGIN=true` | 开发/体验版可见手机号输入，当前保持开启 |

## 项目结构

```text
web-miniapp/
├── miniprogram/
│   ├── app.ts / app.json / app.wxss
│   ├── components/             # fm-photo-picker 等公共组件
│   ├── config/                 # 环境与 API 地址映射（dev/trial/release）
│   ├── custom-tab-bar/         # 按角色渲染 tabBar
│   ├── miniprogram_npm/        # 「构建 npm」生成的 Vant Weapp 产物
│   ├── pages/                  # 登录、首页、报警、隐患、巡查、工单、设备、消息、我的
│   ├── services/               # request 与按域 API
│   ├── store/                  # token / 用户信息（带过期与版本校验）
│   ├── styles/                 # 全局令牌与公共样式
│   └── utils/                  # 格式化、角色守卫、日志、定位、文件地址
├── typings/                    # 小程序全局类型
├── project.config.json
├── project.private.config.json
├── tsconfig.json
└── package.json
```

## 接口地址与环境切换

- 已接入登录、首页、报警/处置、隐患/整改/审核、巡查、工单、设备、消息与图片上传接口；报警处置和工单完成支持 1–3 张现场照片。
- `API_BASE` 按 `wx.getAccountInfoSync().miniProgram.envVersion` 映射：`develop` 用 `DEV_API_BASE`，`trial`/`release` 用 `RELEASE_API_BASE`；`FILE_BASE` 由 `API_BASE` 推导，页面与 WXS 的图片基址统一由 `utils/file.ts` 的 `FILE_DATA` 注入，换域名只改 `config/index.ts` 一处。
- 真机调试需在微信公众平台配置 request 合法域名，或在开发者工具中关闭「校验合法域名」（`urlCheck: false` 短期保留，便于本地 HTTP 调试）。
- 后端本地启动需开启 `FM_WX_MOCK_LOGIN=true` 才能使用开发手机号登录。

## 上传与发布

1. 发布前把 `miniprogram/config/index.ts` 的 `RELEASE_API_BASE` 换成正式 HTTPS 网关（以 `/api/mp` 结尾），并在公众平台配置 request / uploadFile 合法域名；打 tag 发布时 CI 会检查 `RELEASE-API-BASE-NOT-SET` 占位符，未替换将直接失败。
2. 未填写时保留 `RELEASE-API-BASE-NOT-SET.invalid` 占位域名：开发版不受影响；体验版/正式版启动会输出 console 错误并提示「正式 API 域名未配置」，**不会静默连 127.0.0.1**。
3. 开发者工具「上传」体验版并填写版本号，在微信公众平台提交审核，审核通过后发布。
4. 手机号白名单直登当前保持开启（开发/体验版可见），短期不上线；正式对外发布前按合规要求另行评估。

## 待优化（单独排期）

- **主包体积待优化**：16 个页面目前全部注册在主包，分包迁移涉及页面路径与所有 `navigateTo` URL 调整，风险高，暂缓实施。
- 18 个 wxss 中约 108 处硬编码色值待替换为全局样式令牌，属纯样式重构，单独排期。
