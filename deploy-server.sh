#!/usr/bin/env bash
# ============================================================
# ⚡ LightningDeal 宝塔服务器一键部署脚本 (Linux)
# 用法：在项目根目录执行  bash deploy-server.sh
# 前提：已安装 Docker + docker compose 插件
# ============================================================
set -e
cd "$(dirname "$0")"

echo "=============================================="
echo "  ⚡ LightningDeal 宝塔服务器部署"
echo "=============================================="

# ----- 1. 检查 Docker -----
echo "[1/6] 检查 Docker ..."
if ! command -v docker >/dev/null 2>&1; then
  echo "❌ 未检测到 docker。请先安装："
  echo "   方式A（推荐）：宝塔软件商店 → 搜索「Docker管理器」→ 安装"
  echo "   方式B：curl -fsSL https://get.docker.com | bash && systemctl enable --now docker"
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "❌ 未检测到 docker compose 插件。请在宝塔 Docker管理器里装 compose，或："
  echo "   yum install -y docker-compose-plugin"
  exit 1
fi
echo "✅ Docker 就绪：$(docker --version)"

# ----- 2. Elasticsearch 内核参数 -----
echo "[2/6] 检查 vm.max_map_count (Elasticsearch 需要 ≥262144) ..."
CURRENT=$(cat /proc/sys/vm/max_map_count 2>/dev/null || echo 0)
if [ "${CURRENT:-0}" -lt 262144 ]; then
  echo "   当前 $CURRENT < 262144，临时设置中 ..."
  sysctl -w vm.max_map_count=262144
  if grep -q "^vm.max_map_count" /etc/sysctl.conf; then
    sed -i 's/^vm.max_map_count.*/vm.max_map_count=262144/' /etc/sysctl.conf
  else
    echo "vm.max_map_count=262144" >> /etc/sysctl.conf
  fi
  echo "✅ 已设置并写入 /etc/sysctl.conf"
else
  echo "✅ 已满足 ($CURRENT)"
fi

# ----- 3. 构建并启动 -----
echo "[3/6] 构建并启动全部服务（首次拉镜像+编译，约 10~20 分钟，请耐心）..."
docker compose -f docker-compose.btol.yml up -d --build

# ----- 4. 回收构建缓存（磁盘只剩 8G，省空间）-----
echo "[4/6] 回收构建缓存 ..."
docker builder prune -f >/dev/null 2>&1 || true
docker image prune -f   >/dev/null 2>&1 || true

# ----- 5. 服务状态 -----
echo "[5/6] 服务状态："
docker compose -f docker-compose.btol.yml ps

# ----- 6. 等待后端启动 -----
echo "[6/6] 等待后端启动（最多 120 秒）..."
READY=no
for i in $(seq 1 40); do
  if docker logs ld-backend 2>&1 | grep -q "Started LightningDealApplication"; then
    READY=yes; break
  fi
  sleep 3
done

SERVER_IP=$(curl -s --max-time 3 ifconfig.me 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}')
[ -z "$SERVER_IP" ] && SERVER_IP="<服务器公网IP>"

echo ""
echo "=============================================="
if [ "$READY" = "yes" ]; then
  echo "  ✅ 部署完成，后端已就绪！"
else
  echo "  ⚠️  后端尚未打印启动完成日志，可能在等待中间件，已设自动重启。"
  echo "     可执行查看日志：docker compose -f docker-compose.btol.yml logs -f backend"
fi
echo ""
echo "  前端页面:    http://$SERVER_IP:8090"
echo "  Swagger文档: http://$SERVER_IP:8090/api/swagger-ui.html"
echo "  默认账号:    admin / 123456"
echo ""
echo "  管理控制台（仅本机，需 SSH 隧道）："
echo "    RabbitMQ: ssh -L 15672:127.0.0.1:15672 root@$SERVER_IP  → http://localhost:15672 (guest/guest)"
echo "    MinIO:    ssh -L 9001:127.0.0.1:9001   root@$SERVER_IP  → http://localhost:9001 (minioadmin/minioadmin)"
echo ""
echo "  ⚠️  记得在【阿里云安全组】和【宝塔防火墙】放行 8090 端口！"
echo ""
echo "  常用命令（项目根目录）："
echo "    查日志: docker compose -f docker-compose.btol.yml logs -f backend"
echo "    重启:   docker compose -f docker-compose.btol.yml restart"
echo "    停止:   docker compose -f docker-compose.btol.yml down        # 保留数据"
echo "    彻底删: docker compose -f docker-compose.btol.yml down -v     # 连数据一起删"
echo "=============================================="
