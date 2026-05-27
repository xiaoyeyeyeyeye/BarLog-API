# Database scripts

在 PostgreSQL 上**手动**按顺序执行（应用启动不会自动建库）。

## 首次部署

1. 创建数据库：`init-db.sql`（单库）或 `init-databases.sql`（dev/test/prod 三库）
2. 建表与种子：`schema-full.sql`
3. 可选联调数据：`V4_frontend_demo.sql`

## 增量迁移

按版本号顺序执行 `V2_*.sql` … `V6_*.sql`（已部署环境按需执行）。

## 备份（生产建议）

```bash
pg_dump -U postgres -d alcohol -F c -f alcohol_backup.dump
pg_restore -U postgres -d alcohol -c alcohol_backup.dump
```

备份文件勿提交 Git（见根目录 `.gitignore`）。
