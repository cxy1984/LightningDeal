# ===== 构建阶段：Maven 编译打包（内置阿里云镜像源加速）=====
FROM maven:3.6.3-jdk-8 AS build
WORKDIR /app
# settings.xml 含阿里云 central 镜像，加速依赖下载
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ===== 运行阶段：精简 JRE =====
# 若 eclipse-temurin:8-jre-alpine 拉取失败，可改用 amazoncorretto:8-alpine
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=build /app/target/lightning-deal-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
