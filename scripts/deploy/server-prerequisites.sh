#!/bin/bash
# 在服务器上以 root 或 sudo 执行（Ubuntu 22.04 / Debian 12 示例）
# 用途：安装 PostgreSQL、创建三库与用户、创建目录（不写入密码到 Git）
set -euo pipefail

echo "==> 安装 PostgreSQL 与 Nginx、Java 17"
apt-get update
apt-get install -y postgresql postgresql-contrib nginx openjdk-17-jre-headless ufw

echo "==> 创建运行用户与目录"
id alcohol-api &>/dev/null || useradd --system --home /opt/alcohol-api --shell /usr/sbin/nologin alcohol-api
mkdir -p /opt/alcohol-api /var/lib/alcohol-api-dev /var/lib/alcohol-api-test /var/lib/alcohol-api /etc/alcohol-api
chown alcohol-api:alcohol-api /opt/alcohol-api /var/lib/alcohol-api-dev /var/lib/alcohol-api-test /var/lib/alcohol-api

echo "==> 防火墙：仅 SSH + HTTP/S（不开放 5432）"
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable

echo ""
echo "下一步请手动执行（需自设密码，勿写入 Git）："
echo "  1. sudo -u postgres psql -f /opt/alcohol-api/scripts/init-databases.sql"
echo "  2. 为 alcohol_dev / alcohol_test / alcohol_prod 分别执行 schema-full.sql"
echo "  3. sudo cp config/env.server-dev.example /etc/alcohol-api/dev.env && sudo nano /etc/alcohol-api/dev.env"
echo "  4. sudo chmod 600 /etc/alcohol-api/dev.env"
echo "  5. 上传 alcohol-api.jar 到 /opt/alcohol-api/"
echo "  6. 配置 systemd 与 Nginx"
