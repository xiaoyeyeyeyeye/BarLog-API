# 酒卡图片与详情 API（v20260530）

前端联调文档副本，与 BarLog 仓库 `docs/API_SIP_CARD.md` 同步。

## 后端实现

| 能力 | 类/路径 |
|------|---------|
| 真实上传 | `FrontendCompatService.uploadImage` / `uploadCardImage` |
| 酒卡详情 | `SipCardService` → `GET /api/sip-cards/{checkInId}` |
| 静态文件 | `FileStorageService` + `WebConfig` `/uploads/**` |

## 测试

```bash
mvn test -Dtest=FrontendCompatApiTest#getSipCardDetail
```
