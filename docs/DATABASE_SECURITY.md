# BarLog 数据库与 API 安全指南

面向测试/预发布上线前的**最小可行防护**。按优先级从高到低执行。

---

## 1. 当前架构与风险面

```text
互联网
  → Cloudflare Tunnel / Nginx :80
  → spring-api (Docker, 应仅 127.0.0.1:8080)
  → postgres (Docker, 应**不**映射公网 5432)
```

| 威胁 | 现状（加固前常见） | 本项目措施 |
|------|-------------------|------------|
| 暴力破解登录 | 无限制 POST `/api/auth/login` | **IP 限流 + 失败计数**（见 §3） |
| 弱口令注册 | 仅 6 位长度 | **8 位 + 字母 + 数字** |
| SQL 注入 | MyBatis-Plus 参数化查询 | 保持 `#{}` / Lambda，禁止拼接用户输入 |
| PostgreSQL 直连 | **5432 曾对公网开放** | **取消端口映射 + 安全组关 5432** |
| API 绕过 Nginx | **8080 曾对公网开放** | **仅绑定 127.0.0.1:8080** |
| 备份明文泄露 | 无规范 | **加密备份脚本**（见 §5） |
| Swagger 暴露 | dev/test 开启 | prod 关闭；测试环境建议关 |
| 特权提升 | 应用用单一 DB 用户 | 应用账号非 superuser；见 §4 |

---

## 2. 服务器必做（网络层，最重要）

### 2.1 关闭 PostgreSQL 公网入口

**已提供脚本**（在 EC2 上）：

```bash
sudo bash /tmp/harden-docker-network.sh
```

效果：

- `postgres` 容器**不再** `-p 5432:5432`
- `spring-api` 改为 `-p 127.0.0.1:8080:8090`（仅本机 + Nginx 反代）

### 2.2 AWS 安全组（控制台手动）

在 EC2 安全组 **Inbound** 中：

| 端口 | 建议 |
|------|------|
| **5432** | **删除** 对 `0.0.0.0/0` 的规则 |
| **8080** | **删除** 对公网的规则（只留 80/443） |
| **22** | 仅你的办公 IP 或 VPN |
| **80** | 需要对外 Web 时保留 |

### 2.3 强密码与 JWT

编辑 `/opt/alcohol-api/secrets/dev.env`：

```bash
# JWT 至少 32 字符随机串
JWT_SECRET=$(openssl rand -base64 48)

# 数据库密码不要用 postgres_dev / app_pass 这类默认值
SPRING_DATASOURCE_PASSWORD=<强密码>
```

修改 DB 密码后需在 PostgreSQL 内 `ALTER USER ... PASSWORD` 并同步 env，再重建 `spring-api`。

---

## 3. 应用层（已实现，随 JAR 部署）

环境变量（可选，默认已启用）：

```env
SECURITY_RATE_LIMIT_ENABLED=true
SECURITY_AUTH_MAX_RPM=30
SECURITY_MAX_FAILED_LOGINS=10
SECURITY_FAILED_LOGIN_WINDOW_MIN=15
SECURITY_MAX_REGISTER_PER_HOUR=5
SECURITY_PASSWORD_POLICY=true
SECURITY_PASSWORD_MIN_LENGTH=8
```

| 能力 | 说明 |
|------|------|
| `AuthRateLimitFilter` | 限制 login/register/otp/google/start 的 POST 频率 |
| 登录失败计数 | 同一 IP 15 分钟内失败过多 → 429 |
| 注册频率 | 同一 IP 每小时注册次数上限 |
| `PasswordPolicyValidator` | 注册密码强度 |
| `SecurityHeadersFilter` | `X-Frame-Options`、`X-Content-Type-Options` 等 |
| OTP | 原有 cooldown + 日限额（`VerificationCodeService`） |

**SQL 注入**：业务代码使用 MyBatis-Plus `LambdaQueryWrapper`；`.last("LIMIT n")` 仅用于固定数字，勿拼接用户输入。

**多实例**：当前限流在内存中；若以后水平扩展 API，需改为 Redis 限流。

---

## 4. PostgreSQL 权限建议

应用连接用户（如 `alcohol_dev`）应：

- 仅拥有业务库 `CONNECT` + 表 DML 权限
- **不是** `SUPERUSER` / `CREATEDB`
- 不使用 `postgres` 超级用户跑应用

检查：

```bash
docker exec postgres psql -U app_user -d alcohol_dev -c "\du alcohol_dev"
```

测试/生产库分离：`alcohol_test` / `alcohol_prod` 独立用户与密码（见 `scripts/init-databases.sql`）。

---

## 5. 加密备份

```bash
sudo mkdir -p /opt/alcohol-api/backups && sudo chmod 700 /opt/alcohol-api/backups
BACKUP_PASSPHRASE='你的备份口令' sudo -E bash scripts/deploy/backup-db-encrypted.sh alcohol_dev
```

- 输出 `.sql.gz.enc`（AES-256）
- 口令**不要**写入 Git；丢失口令无法恢复
- 定期把加密文件拷到 S3（启用 SSE）或离线存储

---

## 6. 测试版本上线检查清单

- [ ] 5432 不对公网（`ss -tlnp` 无 `0.0.0.0:5432`）
- [ ] 8080 仅 `127.0.0.1` 或未监听公网
- [ ] AWS 安全组已关 5432/8080
- [ ] 测试库使用 `test.env` + `alcohol_test`（与研发 `dev.env` 分离）
- [ ] `COMPAT_ALLOW_ANONYMOUS=false`（测试/生产）
- [ ] CORS 仅允许真实前端域名
- [ ] Swagger 在对外环境关闭（`springdoc.*.enabled=false`）
- [ ] 已配置加密备份与 14 天清理
- [ ] Google / AWS 密钥仅在服务器 env 文件，未进 Git

切换测试环境见 `config/env.server-test.example` 与 `scripts/deploy/switch-server-to-test.sh`。

---

## 7. 相关脚本

| 脚本 | 用途 |
|------|------|
| `scripts/deploy/harden-docker-network.sh` | 关闭 DB/API 公网端口映射 |
| `scripts/deploy/backup-db-encrypted.sh` | 加密逻辑备份 |
| `scripts/deploy/update-google-oauth-env.sh` | OAuth 环境变量更新 |
| `scripts/deploy/switch-server-to-test.sh` | 切换 spring-api 到 `alcohol_test` 测试库 |

---

## 8. 尚未覆盖（后续可增强）

- WAF / Cloudflare Rate Limiting（边缘层）
- Redis 分布式限流
- PostgreSQL SSL + `sslmode=verify-full`
- 审计日志表 / pgAudit
- 漏洞扫描与 `postgres:15` 镜像定期升级
- 数据库行级加密（一般不必，应用层已哈希密码）
