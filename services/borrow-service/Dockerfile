# ============================================================
# borrow-service 多阶段构建
# 构建：docker build -t library/borrow-service:week06 ./services/borrow-service
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

# 使用阿里云镜像，加速容器内 Maven 依赖下载
RUN mkdir -p /root/.m2 && printf '%s\n' \
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">' \
    '  <mirrors>' \
    '    <mirror>' \
    '      <id>aliyunmaven</id>' \
    '      <mirrorOf>central</mirrorOf>' \
    '      <name>Aliyun Maven Central</name>' \
    '      <url>https://maven.aliyun.com/repository/central</url>' \
    '    </mirror>' \
    '  </mirrors>' \
    '</settings>' \
    > /root/.m2/settings.xml

COPY pom.xml .
RUN mvn -B -DskipTests dependency:resolve || echo "dependency warm-up skipped"

COPY src ./src
RUN mvn -B -DskipTests clean package || mvn -B -DskipTests clean package

FROM eclipse-temurin:25-jre

WORKDIR /app

# 非 root 用户运行
RUN useradd -r -u 1001 appuser

COPY --from=build /build/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
