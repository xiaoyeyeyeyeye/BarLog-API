# BarLog — Google OAuth 配置指南（给同事 / AI 助手）

> **用途**：把本文档发给负责 Google Cloud 的同事，或其 AI 助手。  
> AI 读完即可按步骤带同事完成配置，无需了解 BarLog 全部代码。

---

## 1. 任务摘要

| 项目 | 说明 |
|------|------|
| **产品** | BarLog — 酒吧打卡 Web App |
| **当前状态** | 后端 / 前端「Google 登录」代码已部署；**Places API Key 已在 GCP 项目中配置**（Bars 搜索可用） |
| **缺什么** | 同一 GCP 项目中的 **OAuth 2.0 Web 客户端**（Client ID + Client Secret） |
| **不要做什么** | 不要新建 GCP 项目；不要改动现有 Places API Key |

**交付物（通过私聊 / 密码管理器交给开发同学）：**

```env
GOOGLE_CLIENT_ID=<Client ID>
GOOGLE_CLIENT_SECRET=<Client Secret>
```

---

## 2. 凭证类型（避免搞错）

| 类型 | 用途 | 本次 |
|------|------|------|
| **API Key** | Google Places 酒吧搜索 | 已有，勿动 |
| **OAuth 2.0 Client ID + Secret** | 用户用 Google 账号注册 / 登录 | **本次创建** |

API Key **不能**代替 OAuth Client ID。

---

## 3. 登录流程（理解 Redirect URI 填什么）

```text
用户点击「Enter with Google」
  → 后端返回 Google 授权链接
  → 用户在 Google 页面授权
  → Google 回调【后端】/api/auth/google/callback   ← 必须在 Console 注册
  → 后端签发 JWT，跳回【前端】/auth/google/callback
```

因此在 Google Cloud Console 的 **Authorized redirect URIs** 必须填 **后端地址**（不是前端页面路径）。

**当前研发环境回调地址（逐字一致）：**

```text
https://canyon-independently-constitution-can.trycloudflare.com/api/auth/google/callback
```

---

## 4. 操作步骤

### Step 0 — 登录并选择正确项目

1. 打开 [Google Cloud Console](https://console.cloud.google.com/)
2. 使用 **管理现有 Places API Key 的 Google 账号** 登录
3. 顶部项目选择器 → 选中 **包含 Places API Key 的项目**

**验证**：**APIs & Services → Credentials** 中应能看到已有 API Key（形如 `AIzaSy...`）。

---

### Step 1 — OAuth 同意屏幕（OAuth consent screen）

路径：**APIs & Services → [OAuth consent screen](https://console.cloud.google.com/apis/credentials/consent)**

若尚未配置：

| 字段 | 建议值 |
|------|--------|
| User type | **External** |
| App name | `BarLog` |
| User support email | 操作者邮箱 |
| Developer contact | 操作者邮箱 |
| Scopes | 包含 **openid、email、profile**（默认即可） |

**Testing 阶段**：在 **Test users** 中加入需要测试的 Gmail（未发布前仅测试用户可登录）。

若同意屏幕已存在，确认状态为 Testing 或 In production；Testing 时务必添加 Test users。

---

### Step 2 — 创建 OAuth 客户端

路径：**APIs & Services → [Credentials](https://console.cloud.google.com/apis/credentials) → Create credentials → OAuth client ID**

| 字段 | 值 |
|------|-----|
| Application type | **Web application** |
| Name | `BarLog Web Dev`（可自定） |

**Authorized redirect URIs（必填，精确匹配）：**

```text
https://canyon-independently-constitution-can.trycloudflare.com/api/auth/google/callback
```

**Authorized JavaScript origins（建议填写）：**

```text
https://canyon-independently-constitution-can.trycloudflare.com
```

点击 **Create**。

---

### Step 3 — 保存并交付凭证

创建成功后复制：

- **Client ID**（形如 `123456789-xxxx.apps.googleusercontent.com`）
- **Client Secret**（仅显示一次，务必保存）

**安全交付示例：**

```text
GOOGLE_CLIENT_ID=xxxxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxx
GCP 项目名：<项目名>
OAuth 客户端名：BarLog Web Dev
Redirect URI 已配置：https://canyon-independently-constitution-can.trycloudflare.com/api/auth/google/callback
```

**禁止**：提交 Git、发公开群、写进前端代码。

---

## 5. 开发同学收到后会做什么

写入服务器 `/opt/alcohol-api/secrets/dev.env`（示例）：

```env
GOOGLE_CLIENT_ID=<同事提供的 Client ID>
GOOGLE_CLIENT_SECRET=<同事提供的 Client Secret>
GOOGLE_OAUTH_CALLBACK_URL=https://canyon-independently-constitution-can.trycloudflare.com/api/auth/google/callback
GOOGLE_OAUTH_ALLOWED_REDIRECT_ORIGINS=https://canyon-independently-constitution-can.trycloudflare.com
```

重建 `spring-api` Docker 容器（`docker restart` 不会重读 `--env-file`）。

---

## 6. 验收标准

1. 打开 BarLog 登录 / 注册页  
2. 点击 **Enter with Google**  
3. 出现 Google 账号授权页  
4. 授权后回到 App 并完成登录  

---

## 7. 常见问题

| 现象 | 原因 / 处理 |
|------|-------------|
| `redirect_uri_mismatch` | Console 中 Redirect URI 与上文不一致（http/https、路径、多余斜杠） |
| `Google sign-in is not configured` | 服务器尚未写入 Client ID / Secret |
| 测试账号无法登录 | OAuth 应用为 Testing，需将该 Gmail 加入 **Test users** |
| Access blocked | 同意屏幕未配置或用户不在 Test users |
| Bars 搜索正常但 Google 登录不行 | 正常：Places 用 API Key，登录用 OAuth，两套凭证 |

---

## 8. 给 AI 助手的执行清单

请按顺序引导同事，并在每步完成后勾选：

- [ ] 已登录正确的 GCP 项目（能看到 Places API Key）
- [ ] OAuth consent screen 已配置（含 Test users，如为 Testing）
- [ ] 已创建 **Web application** 类型 OAuth 客户端
- [ ] Redirect URI 与第 3 节 **完全一致**
- [ ] 已安全交付 Client ID + Client Secret
- [ ] 已告知开发同学，等待服务器更新与验收

---

## 9. 相关文档

| 文档 | 说明 |
|------|------|
| [GOOGLE_PLACES_SETUP.md](./GOOGLE_PLACES_SETUP.md) | Places API Key（已完成） |
| [API_GOOGLE_AUTH.md](./API_GOOGLE_AUTH.md) | 后端 OAuth API 与环境变量 |
| 前端 `BarLog-main/docs/API_GOOGLE_AUTH.md` | 前端契约 |

---

## 10. 域名变更说明

若 Cloudflare 隧道域名更换，需同时更新：

1. Google Console 中 OAuth 客户端的 **Authorized redirect URIs**
2. 服务器 `GOOGLE_OAUTH_CALLBACK_URL` 与 `GOOGLE_OAUTH_ALLOWED_REDIRECT_ORIGINS`

然后重建 `spring-api` 容器。
