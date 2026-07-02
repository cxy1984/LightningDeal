# ===== 构建阶段：Node 编译 Vue 前端 =====
FROM node:16-alpine AS build
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
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
