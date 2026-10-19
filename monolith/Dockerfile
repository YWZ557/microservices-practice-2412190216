# ============================================================
# 多阶段构建：构建阶段用 Maven + JDK 25，运行阶段只用 JRE 25
# 构建：docker build -t monolith:week05 ./monolith
# ============================================================

# ---------- 阶段一：构建 ----------
# 若该标签拉取失败，可依次尝试：
#   maven:3-eclipse-temurin-25
#   eclipse-temurin:25-jdk（改用 ./mvnw 构建）
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

# 配置 Maven 镜像源：容器内直连 repo.maven.apache.org 速度只有 40~70 kB/s，
# Spring Boot 4 + JPA + MyBatis 有数百个依赖，直连要十几分钟。
# 这里改为阿里云镜像，构建时间通常缩短到原来的 1/5 左右。
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

# 先只拷贝 pom，单独一层缓存 Maven 依赖，改源码时不必重新下载。
# 这里刻意不使用 dependency:go-offline：该目标要解析整棵插件依赖树，
# 容器内网络稍有抖动就会以 PluginResolutionException 直接失败，
# 反而让整个构建无法完成。改用 dependency:resolve，只拉取编译期依赖，
# 失败也不阻断构建，真正的依赖在下一次 package 时补齐。
COPY pom.xml .
RUN mvn -B -DskipTests dependency:resolve || echo "dependency warm-up skipped"

# 再拷贝源码并打包（跳过测试：测试类需要连接数据库，构建阶段没有数据库）。
# 失败后自动重试一次，规避容器内偶发的依赖下载中断。
COPY src ./src
RUN mvn -B -DskipTests clean package || mvn -B -DskipTests clean package

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

# 非 root 用户运行，降低容器逃逸风险
RUN useradd -r -u 1001 appuser

# 只把可执行 JAR 从构建阶段拷过来，镜像内不含 Maven 与源码
COPY --from=build /build/target/monolith-*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
