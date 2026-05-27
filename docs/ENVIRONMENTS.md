# 环境划分与敏感信息管理

BarLog 后端采用 **Spring Profile**（`dev` / `test` / `prod`）。  
敏感配置通过环境变量注入，**不写入 Git**。

---

## 1. 三套环境对照

| 项目 | 研发 dev | 测试 test | 生产 prod |
|------|----------|-----------|-----------|
| **Profile** | `dev`（默认） | `test` | `prod` |
| **配置文件** | `application-dev.yml` | `application-test.yml` | `application-prod.yml` |
| **数据库名** | `alcohol`（本机已有）或 `alcohol_dev` | `alcohol_test` | `alcohol_prod` |
| **API 域名** | `http://localhost:8090` | `https://api-test.barlog.app` | `https://api.barlog.app` |
| **前端域名** | `http://localhost:8081` | `https://app-test.barlog.app` | `https://app.barlog.app` |
| **匿名访问** | 开启（便于 Web 演示） | 关闭 | **必须关闭** |
| **Swagger** | 开启 | 开启 | **关闭** |
| **JWT / DB 密码** | 可本机默认值 | 独立密钥 | 独立强密钥 |

---

## 2. 为什么要三个数据库？

**是的，建议在服务器上建三个独立数据库**（同一 PostgreSQL 实例即可，不同库名 + 不同 DB 用户）：

| 库名 | 用途 |
|------|------|
| `alcohol` 或 `alcohol_dev` | 本机研发 |
| `alcohol_test` | 测试/预发布，可随意灌测试数据 |
| `alcohol_prod` | 生产真实用户数据 |

好处：测试不会误删生产数据；迁移脚本可先在 test 验证；备份策略可分开。

初始化步骤见 `scripts/init-databases.sql`，每个库执行一次 `scripts/schema-full.sql`。

---

## 3. 什么不能进 Git？

| 可以提交 Git | **禁止提交 Git** |
|--------------|------------------|
| `application-*.yml`（只有占位符 `${DB_PASSWORD}`） | 真实 DB 密码 |
| `config/env.example`（模板） | `config/prod.env`、`/etc/alcohol-api/*.env` |
| `config/env.*.example` | 本机 `alcohol-spring-boot/.env` |
| 前端 `.env.example` | 含真实密钥的 `.env` |

原则：**仓库里只有「变量名 + 示例占位符」，真实值只在服务器或本机私密文件。**

已在 `.gitignore` 忽略：

```
.env
.env.local
config/*.env
!config/env.example
!config/env.*.example
```

---

## 4. 敏感信息在服务器上怎么存？

### 推荐：systemd + EnvironmentFile

1. 在服务器创建目录（仅 root 可读）：

```bash
sudo mkdir -p /etc/alcohol-api
sudo chmod 700 /etc/alcohol-api
```

2. 从模板复制并填写真实值：

```bash
sudo cp config/env.example /etc/alcohol-api/prod.env
sudo nano /etc/alcohol-api/prod.env
sudo chmod 600 /etc/alcohol-api/prod.env
```

3. 使用示例 unit 文件 `scripts/systemd/alcohol-api@.service.example`：

```bash
sudo systemctl enable alcohol-api@prod
sudo systemctl start alcohol-api@prod
```

测试环境同理：`/etc/alcohol-api/test.env` + `alcohol-api@test`。

### 变量说明（生产必填）

| 变量 | 说明 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | JDBC 连接串，指向 `alcohol_prod` |
| `DB_USERNAME` / `DB_PASSWORD` | 生产 DB 用户 |
| `JWT_SECRET` | 至少 32 字符随机串，**与 test 不同** |
| `CORS_ALLOWED_ORIGINS` | 前端域名，逗号分隔 |
| `UPLOAD_DIR` | 上传目录绝对路径 |

生成 JWT 密钥示例：

```bash
openssl rand -base64 48
```

### 其他可选方式

| 方式 | 适用场景 |
|------|----------|
| **Docker Compose `env_file`** | 容器部署 |
| **云厂商密钥管理**（AWS SSM、Vault） | 多机/合规要求高 |
| **CI/CD Secret** | 自动部署时注入，不落地到仓库 |

---

## 5. 本地研发怎么启动

```powershell
cd alcohol-spring-boot
# 默认 dev profile，连本机 alcohol 库
mvn spring-boot:run

# 或显式指定
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

可选：复制 `config/env.dev.example` → `.env`，用工具 `source` 或手动 `$env:DB_PASSWORD="..."` 后再启动。

---

## 6. 测试 / 生产服务器启动

```bash
# 打包
mvn -DskipTests package

# 测试（环境变量已从 test.env 注入时）
java -jar target/alcohol-api-*.jar --spring.profiles.active=test

# 生产
java -jar target/alcohol-api-*.jar --spring.profiles.active=prod
```

---

## 7. 前端环境变量

| 文件 | API 地址 | 是否提交 Git |
|------|----------|--------------|
| `.env.development` | `http://localhost:8090` | 可提交（无秘密） |
| `.env.staging` | `https://api-test.barlog.app` | 建议用 `.env.staging.example` 提交模板 |
| `.env.production` | `https://api.barlog.app` | 同上 |

启动 / 构建：

```bash
# 研发 Web
npx expo start --web

# 测试包（需本地 .env.staging）
npx expo export --platform web --env-file .env.staging
```

`EXPO_PUBLIC_*` 会打进前端包，**不是秘密**；真正秘密只在后端。

---

## 8. Cloudflare 简要（与 profile 配合）

| 域名 | 环境 | 后端 Profile |
|------|------|--------------|
| `api-test.barlog.app` | 测试 | `test` |
| `api.barlog.app` | 生产 | `prod` |

- SSL 模式：**Full (strict)**
- `/api/*`：**Bypass cache**
- 源站 Nginx 设置 `X-Forwarded-*`，后端已开 `forward-headers-strategy: framework`（test/prod）

---

## 9. 检查清单（上线前）

- [ ] `alcohol_test`、`alcohol_prod` 已建库并执行 `schema-full.sql`
- [ ] `/etc/alcohol-api/prod.env` 权限 `600`，未进 Git
- [ ] `JWT_SECRET`、DB 密码 test/prod **各不相同**
- [ ] `COMPAT_ALLOW_ANONYMOUS=false`（test/prod 默认已关）
- [ ] 前端生产构建指向 `https://api.barlog.app`
- [ ] Cloudflare API 路径不缓存
