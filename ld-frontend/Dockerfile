# ===== 构建阶段：Node 编译 Vue 前端 =====
FROM node:20-alpine AS build
WORKDIR /app
# 先装依赖（利用层缓存），用国内镜像加速
COPY package.json ./
RUN npm config set registry https://registry.npmmirror.com \
    && npm install
# 再拷源码并构建
COPY . .
RUN npm run build

# ===== 运行阶段：Nginx 托管静态资源 + 反代后端 =====
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 重写主配置：pid 改到 /tmp（非 root 用户可写），解决新版 nginx:alpine 权限问题
RUN echo 'user  nginx; worker_processes  auto; error_log  /var/log/nginx/error.log notice; pid  /tmp/nginx.pid; events { worker_connections  1024; } http { include /etc/nginx/mime.types; include /etc/nginx/conf.d/*.conf; }' > /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
