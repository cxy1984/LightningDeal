@echo off
chcp 65001 >nul
title LightningDeal 一键部署

echo ============================================
echo   ⚡ LightningDeal 一键部署脚本
echo ============================================
echo.

:: ===== 1. 检查 Docker 是否运行 =====
echo [1/5] 检查 Docker 状态...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker 未运行！请先启动 Docker Desktop
    pause
    exit /b 1
)
echo ✅ Docker 运行正常
echo.

:: ===== 2. 构建后端 =====
echo [2/5] 构建后端项目...
cd /d "%~dp0ld-backend"
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ❌ 后端构建失败！
    pause
    exit /b 1
)
echo ✅ 后端构建成功
echo.

:: ===== 3. 构建前端 =====
echo [3/5] 构建前端项目...
cd /d "%~dp0ld-frontend"

:: 检测包管理器
if exist node_modules\ (
    echo 依赖已安装，跳过 install
) else (
    if exist pnpm-lock.yaml (
        call pnpm install
    ) else if exist yarn.lock (
        call yarn install
    ) else (
        call npm install
    )
)

call npm run build
if %errorlevel% neq 0 (
    echo ❌ 前端构建失败！
    pause
    exit /b 1
)
echo ✅ 前端构建成功
echo.

:: ===== 4. 启动所有服务 =====
echo [4/5] 启动 Docker 服务（首次会拉取镜像，需要几分钟）...
cd /d "%~dp0"
docker compose up -d --build
if %errorlevel% neq 0 (
    echo ❌ Docker 服务启动失败！
    pause
    exit /b 1
)
echo ✅ 所有服务已启动
echo.

:: ===== 5. 等待后端就绪 =====
echo [5/5] 等待后端启动...
:wait
timeout /t 3 /nobreak >nul
curl -s http://localhost:8080/api/actuator/health >nul 2>&1
if %errorlevel% neq 0 (
    goto wait
)
echo ✅ 后端已就绪
echo.

echo ============================================
echo   ✅ 部署完成！
echo.
echo   后端 API:     http://localhost:8080/api
echo   前端页面:     http://localhost:5173
echo   Swagger文档:  http://localhost:8080/api/swagger-ui.html
echo   RabbitMQ管理: http://localhost:15672 (guest/guest)
echo   MinIO控制台:  http://localhost:9001 (minioadmin/minioadmin)
echo ============================================

pause
