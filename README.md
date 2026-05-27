# BarLog API

BarLog（余味）后端服务：酒吧打卡、日记、地图 POI（Google Places）、用户与认证。

## Stack

- Java 17 · Spring Boot 3.1 · MyBatis-Plus · PostgreSQL
- JWT · SpringDoc OpenAPI

## Quick start

```bash
# 1. PostgreSQL：创建库并执行 scripts/schema-full.sql
# 2. 可选：复制 config/env.dev.example → .env 并填写 DB 密码
mvn spring-boot:run
```

默认 `dev` profile，端口 **8090**。健康检查：`GET /health`

| Profile | 配置 | 数据库 |
|---------|------|--------|
| `dev` | `application-dev.yml` | `alcohol` |
| `test` | `application-test.yml` | `alcohol_test` |
| `prod` | `application-prod.yml` | 须设置 `DB_*` / `JWT_SECRET` 环境变量 |

## API

| 层 | 路径前缀 | 说明 |
|----|----------|------|
| 前端契约 | `/api/auth`, `/api/bars`, `/api/checkins`, … | JSON 直出，对齐 Expo 客户端 |
| 内部 API | `/api/internal/*` | `{ code, msg, result }` 包装 |

- Swagger（dev/test）：http://localhost:8090/swagger-ui.html
- 契约说明：[docs/FRONTEND_API_CONTRACT.md](docs/FRONTEND_API_CONTRACT.md)
- Google Places：[docs/GOOGLE_PLACES_SETUP.md](docs/GOOGLE_PLACES_SETUP.md)
- 环境变量：[docs/ENVIRONMENTS.md](docs/ENVIRONMENTS.md)

## Database scripts

| 脚本 | 用途 |
|------|------|
| `scripts/schema-full.sql` | 全量建表 + 种子数据 |
| `scripts/init-databases.sql` | dev/test/prod 三库与用户（占位密码） |
| `scripts/V4_frontend_demo.sql` | 联调用酒吧 demo 数据 |

## Test

```bash
mvn test
powershell -File scripts/smoke-compat-api.ps1   # 需先启动服务
```

## Config templates

`config/env.example` · `config/env.dev.example` · `config/env.server-dev.example`  
真实密钥只放在本机 `.env` 或服务器 secrets，勿提交 Git。
