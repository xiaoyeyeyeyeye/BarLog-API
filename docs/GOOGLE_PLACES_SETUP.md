# Google Places API 配置指南

后端通过 **Places API (New)** 代理酒吧搜索与详情；API Key **仅**放在服务器环境变量，勿提交 Git。

## 1. 需要 Key 吗？免费还是付费？

**需要 Key，且必须绑定 Google Cloud 结算账号（Billing）。**

自 **2025-03-01** 起，Google Maps Platform 改为按 SKU 分档的**每月免费额度**，不再是统一的 $200 月赠金：

| 档位 | 每 SKU 每月免费调用量（约） |
|------|---------------------------|
| Essentials | 10,000 次 |
| Pro | 5,000 次 |
| Enterprise | 1,000 次 |

我们当前使用的接口（Text Search / Nearby Search / Place Details）因 field mask 含 `rating`、`regularOpeningHours` 等，多数落在 **Pro / Enterprise** 档，免费额度相对更少。**超出免费额度后按量计费**（具体单价见 [官方价目](https://mapsplatform.google.com/pricing/)）。

新 GCP 项目通常还有 **$300 试用赠金**（一次性，有期限）。

**结论**：研发联调在合理使用下成本很低；**不设防护的话，Expo 热重载、爬虫或误配前端直连 Key 确实可能刷出意外账单**。

---

## 2. 推荐防护策略（本项目已实现 + 建议在 GCP 做）

### 后端（已实现）

| 机制 | 配置项 | 说明 |
|------|--------|------|
| Key 仅服务端 | `GOOGLE_PLACES_API_KEY` | 禁止写入 Expo / Git |
| 开关 | `GOOGLE_PLACES_ENABLED` | 关则全程用 seed 数据 |
| 列表缓存 | `GOOGLE_PLACES_CACHE_TTL_SECONDS=300` | 5 分钟内同 city/坐标不重复搜 |
| 详情缓存 | `GOOGLE_PLACES_DETAIL_CACHE_TTL_SECONDS=86400` | 24h 内同酒吧不重复 Details |
| **每日上限** | `GOOGLE_PLACES_DAILY_REQUEST_LIMIT=1000` | 超限后自动降级 seed，打 WARN 日志 |
| 预警日志 | `GOOGLE_PLACES_WARN_THRESHOLD_PERCENT=80` | 用到 80% 时告警 |

### Google Cloud Console（务必做）

1. Key **API 限制**：只允许 Places API (New)
2. Key **IP 限制**：仅 EC2 公网 IP
3. **Budget 预算告警**：建议 dev $20–50/月，prod 按 DAU 估
4. 在 [Quotas](https://console.cloud.google.com/google/maps-apis/quotas) 可设每日请求上限（硬顶）

### 不建议

- 把 Key 放进 Expo 前端（可被提取滥用）
- field mask 用 `*` 拉全字段（费用暴涨）
- 每次列表点击都调 Details（应用详情缓存 + 列表已有基础字段）

---

## 3. 酒吧详情接口（前端尚未接 UI，后端已就绪）

```http
GET /api/bars/{barId}?lat=1.29&lng=103.85
```

与列表相同的 `Bar` 形状，详情额外字段：

| 字段 | 说明 |
|------|------|
| `description` | seed：类型标签；Google：primaryType 简述 |
| `openingHours` | 营业时间（Google 为完整 week 描述） |
| `checkInCount` | BarLog 本地打卡数 |
| `reviewCount` | Google 评价数 |
| `websiteUrl` | 官网（Google） |
| `googleMapsUrl` | Google Maps 链接（后续 UI 可跳转 / 归属展示） |
| `source` | `google` \| `seed` |

列表项 `id` 为 `gp_xxx` 时，详情走 Google Place Details；失败或未配 Key 则 404 或回落 seed。

---

## 4. Google Cloud Console 配置步骤

1. 打开 [Google Cloud Console](https://console.cloud.google.com/)
2. 创建或选择项目
3. **APIs & Services → Library** → 启用 **Places API (New)**
4. **Billing** → 绑定结算账号 + 设置 Budget 告警

## 5. 创建 API Key

1. **Credentials → Create credentials → API key**
2. 限制 Key：仅 Places API (New) + EC2 IP 白名单
3. 写入服务器 secrets

## 6. 服务器环境变量

编辑 `/opt/alcohol-api/secrets/dev.env`（`chmod 600`）：

```bash
GOOGLE_PLACES_ENABLED=true
GOOGLE_PLACES_API_KEY=你的API密钥
GOOGLE_PLACES_DEFAULT_CITY=Singapore
GOOGLE_PLACES_SEARCH_RADIUS_M=3000
GOOGLE_PLACES_DAILY_REQUEST_LIMIT=1000
GOOGLE_PLACES_CACHE_TTL_SECONDS=300
GOOGLE_PLACES_DETAIL_CACHE_TTL_SECONDS=86400
```

重启：`sudo docker restart spring-api`

## 7. 验证

```bash
# 列表
curl -s "http://127.0.0.1:8080/api/bars/nearby?city=Singapore" | head -c 400

# 详情（将 gp_xxx 换成列表返回的 id）
curl -s "http://127.0.0.1:8080/api/bars/gp_xxx" | head -c 600
```

## 8. 费用粗算（研发期）

假设每天 50 次列表 + 20 次详情 ≈ 70 次 Google 请求/天 ≈ **2,100 次/月**，通常在 Pro SKU 免费额度（约 5,000/月）内。**真正危险的是无缓存 + 热重载每秒多次请求**，所以务必保留 `DAILY_REQUEST_LIMIT` 与缓存。

Field mask 说明：[Choose fields to return](https://developers.google.com/maps/documentation/places/web-service/choose-fields)
