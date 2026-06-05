# 前端 API 契约（BarLog Expo ↔ Spring Boot compat）

与 `BarLog-main/mock-server/server.mjs`、`src/services/api/endpoints.ts`、`src/types/domain.ts` 对齐。

## 通用约定

| 项 | 约定 |
|----|------|
| Base URL | `EXPO_PUBLIC_API_BASE_URL`（如 `http://host:8080`） |
| 路径前缀 | `/api/...`（health 为 `/health`） |
| 成功响应 | **直接 JSON**，无 `{ code, data }` 包装 |
| 错误响应 | `{ "message": string, "code": string }` |
| 认证 | `Authorization: Bearer <accessToken>`；**dev 服务器默认 `COMPAT_ALLOW_ANONYMOUS=false`**，无 Token → `401 AUTH_REQUIRED` |
| 时间 | ISO-8601 UTC 字符串（如 `2026-05-22T14:20:00Z`） |
| 分页列表 | `{ "items": T[], "nextCursor"?: string }` |
| 评分 | 前端 **1.0–5.0**（一位小数）；库内 **1–10** 整数，compat 层自动换算 |
| 枚举 | **小写 + 下划线**（如 `cocktail`、`open_to_chat`、`tonight_only`） |
| 城市 | 前端传 `Shanghai`，库内可存 `上海`，响应统一为 `Shanghai` |

## P0 接口清单

### 健康

- `GET /health` → `{ "ok": true, "service": "..." }`

### 认证

| 方法 | 路径 | 请求 | 响应 |
|------|------|------|------|
| POST | `/api/auth/login` | `{ email, password }` | `{ user, accessToken, refreshToken? }` |
| POST | `/api/auth/register` | `{ displayName, email, password }` | 201，同上 |
| POST | `/api/auth/logout` | — | `{}` |
| GET | `/api/auth/me` | — | `User` |
| POST | `/api/auth/refresh` | `{ refreshToken? }` | `AuthResponse` |

`User`: `{ id, displayName, email?, avatarUrl?, city?, persona? }`

### 打卡 Check-in

**私人日记接口必须携带 Bearer Token，且仅返回/操作当前登录用户的数据。**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/checkins/recent` | **需登录**；`{ items: CheckIn[] }` 仅含 **当前用户** 打卡 |
| POST | `/api/checkins` | **需登录**；body: `CreateCheckInPayload`，201 → `CheckIn` |
| GET | `/api/checkins/{id}` | **需登录**；本人任意 visibility；他人仅 `public`/`tonight_only` 且未过期，否则 `403 CHECKIN_FORBIDDEN` |
| DELETE | `/api/checkins/{id}` | **需登录**；仅本人可删 |
| GET | `/api/users/{userId}/checkins` | **需登录**；`userId` 必须等于当前用户，否则 `403` |

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/bars/{barId}/checkins` | 仅返回该 bar 下 **公开且未过期** 的打卡（不含 private） |

`CheckIn` 字段见 `domain.ts`；`moodTags` 始终为数组（可为 `[]`）。

### 日记 Diary

| 方法 | 路径 | Query | 响应 |
|------|------|-------|------|
| GET | `/api/diary/summary` | `month=yyyy-MM` | `DiarySummary` |
| GET | `/api/diary/calendar` | `month` | `DiaryCalendarDay[]` |
| GET | `/api/diary/stats` | — | `{ categoryCounts, moodCounts }` |

`DiarySummary`: `{ month, checkInCount, barsVisited, averageRating?, currentStreak? }`

### 酒吧 Bars

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/bars/nearby` | `?city=` 或 `?lat=&lng=&radiusMeters=&query=` → `{ items: Bar[], source, message? }`；`query` 有值时走 Google Places **Text Search (New)** |
| GET | `/api/bars/rankings` | 同上，按 `rating` 降序 |
| GET | `/api/bars/{id}` | `BarDetail`（含 `checkInCount`）；可选 `?lat=&lng=` |
| GET | `/api/bars/{id}/checkins` | `{ items: CheckIn[] }` |

**数据源（Google Places）**

- 环境变量 `GOOGLE_PLACES_ENABLED=true` 且配置 `GOOGLE_PLACES_API_KEY` 时，优先调用 [Places API (New)](https://developers.google.com/maps/documentation/places/web-service/overview)：
  - 有 `lat`+`lng` → Nearby Search（`bar` / `night_club`）
  - 仅有 `city` → Text Search（`bars in {city}`）
- Google 酒吧 `id` 前缀为 **`gp_`**（后接 Google `place_id`）；seed 数据仍为 `bar_001` 等
- Google `rating` 为 1.0–5.0，**不做 /2 换算**；seed 库内 `avg_rating` 仍为 1–10 分制
- `checkInCount` 始终来自本地 `check_ins` 聚合
- Key 未配置或 Google 失败 → **自动回落** PostgreSQL 种子酒吧
- 配置说明见 [`docs/GOOGLE_PLACES_SETUP.md`](GOOGLE_PLACES_SETUP.md)

### 上传 / 酒品 / 人格

| 方法 | 路径 | 响应 |
|------|------|------|
| POST | `/api/uploads/image` | `{ imageUrl, width, height, mimeType }` |
| GET | `/api/drinks/collection` | `DrinkCollectionItem[]` |
| GET | `/api/drinks/{id}` | 单条或 404 |
| GET | `/api/persona/current` | `{ id, statement, traits[], updatedAt }` |

### Gallery / Match / Chat / AI

Gallery 与 Community 共用打卡数据；Match/AI 仍为 compat 占位；**Chat 已持久化**（REST + WebSocket）。

#### Gallery（兼容旧路径）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/gallery/feed` | `?range=12h\|24h\|7d\|30d` → `{ items: GalleryPost[], nextCursor? }`；**需登录**；全球公开打卡，不含城市/酒吧字段 |

#### Community（新）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/community/eligibility` | → `{ canViewCommunity: true, todayCheckInId?, todayBarId?, todayCity? }`（`canView*` 恒为 `true`；`today*` 仅表示今日是否已打卡，供 UI 展示） |
| GET | `/api/community/feed` | `?range=&cursor=&limit=` → 全球 Feed；**需登录** |
| POST | `/api/community/posts/{checkInId}/like` | toggle 点赞 → `{ liked, likedCount }` |
| GET | `/api/community/posts/{checkInId}/comments` | `{ items: Comment[] }` |
| POST | `/api/community/posts/{checkInId}/comments` | `{ body }` → `Comment` |
| POST | `/api/community/users/{userId}/wave` | `{ checkInId? }` → `{ conversationId, status }` |

**Ephemeral 规则**

- 打卡 `visibility` 为 `public` 或 `tonight_only` 时，`expires_at = now + 24h`（可配置 `GALLERY_DEFAULT_HOURS`）
- Feed 只返回 `expires_at > now` 的记录；过期后自动从列表消失

**CommunityPost** 在 GalleryPost 基础上扩展：`barId`, `socialStatus`, `visibility`, `likedCount`, `commentCount`, `likedByMe`, `expiresAt`, `avatarUrl`

#### Chat（REST + WebSocket）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat/conversations` | `{ items: Conversation[] }` |
| GET | `/api/chat/conversations/{id}/messages` | `?cursor=&limit=` → `{ items: ChatMessage[], nextCursor? }` |
| POST | `/api/chat/conversations/{id}/messages` | `{ body }` → `ChatMessage` |
| POST | `/api/chat/conversations/{id}/read` | 清空未读 |

WebSocket：`ws(s)://host/ws/chat?token=<accessToken>`（原生 JSON 帧，非 STOMP）

- 服务端推送：`{ "type": "message.new", "conversationId", "message" }`、`{ "type": "message.read", ... }`

## 枚举映射（后端存储 ↔ 前端）

| 前端 | 数据库存储（`check_ins.social_status` 等） |
|------|---------------------------------------------|
| `watercolor` | `WATERCOLOR` |
| `receipt` | `RECEIPT` |
| `film_ticket` | `FILM_TICKET` |
| `doodle_glow` | `DOODLE_GLOW` |
| `passport_stamp` | `PASSPORT_STAMP` |
| `cocktail` 等 | `COCKTAIL` 等大写；空/未知 → 响应 `other` |
| `public` / `private` / `tonight_only` | `PUBLIC` / `PRIVATE` / `TONIGHT_ONLY` |
| `not_social` | `NONE`（兼容旧值 `NOT_SOCIAL`） |
| `open_to_chat` | `CHAT_OK`（兼容 `OPEN_TO_CHAT`） |
| `looking_for_buddy` | `FIND_BUDDY`（兼容 `LOOKING_FOR_BUDDY`） |
| `friends_only` | `VIEW_ONLY`（兼容 `FRIENDS_ONLY`） |

### 评分与统计

- 写入：`rating` 前端 1.0–5.0 → 库内 `round(rating * 2)`（1–10）
- 读出：`round(dbRating / 2, 1)`；酒吧 `avg_rating` 同理
- `DiarySummary.averageRating` / `currentStreak` 由 `CheckInStatsUtil` 从真实打卡计算，无硬编码占位

### 列表与空值

- `moodTags`：响应始终为 JSON 数组（`[]` 合法）；创建时 `@NotNull`，可传 `[]`
- `categoryCounts` / `moodCounts`：无数据时为 `{}`，不是 `null`
- 分页形态：`{ "items": [...] }`；酒吧附近/榜单为 **顶层数组** `Bar[]`（与 mock-server 一致）

### Gallery 数据来源

- Feed 仅包含 `visibility` 为 `PUBLIC` 或 `TONIGHT_ONLY` 的打卡
- 研发演示数据见 `scripts/V5_demo_checkins.sql`（`social_status` 使用 `CHAT_OK` / `NONE`）

## 测试

```bash
mvn test -Dtest=FrontendCompatApiTest
powershell -File scripts/smoke-compat-api.ps1
```
