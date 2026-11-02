# syntax=docker/dockerfile:1
# ============================================================
# 通用构建文件：通过 --build-arg MODULE 指定要打包的模块
# 用法由 compose.yaml 传入，四个服务共用这一个 Dockerfile
# ============================================================

# ---------------- 构建阶段 ----------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# 只拷源码；target/ 与 nacos/ 由 .dockerignore 排除
COPY . .

# 由 compose 的 build.args 传入，例如 library-book
# 挂载 Maven 本地仓库缓存，四个服务共享，避免重复下载依赖
ARG MODULE
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests -pl ${MODULE} -am package \
 && cp ${MODULE}/target/*.jar /workspace/app.jar

# ---------------- 运行阶段 ----------------
FROM eclipse-temurin:25-jre
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080 8081 8082 8083
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
