# ============================================
# 探店路线规划系统 - 后端 Dockerfile
# 多阶段构建：构建阶段 + 运行阶段
# ============================================

# 阶段1: 构建阶段
FROM maven:3.8-openjdk-8-slim AS builder

# 限制 Maven 构建内存
ENV MAVEN_OPTS="-Xms128m -Xmx384m"

# 配置阿里云 Maven 镜像（加速国内下载）
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings>' >> /root/.m2/settings.xml && \
    echo '  <mirrors>' >> /root/.m2/settings.xml && \
    echo '    <mirror>' >> /root/.m2/settings.xml && \
    echo '      <id>aliyun</id>' >> /root/.m2/settings.xml && \
    echo '      <mirrorOf>central</mirrorOf>' >> /root/.m2/settings.xml && \
    echo '      <name>Aliyun Maven Mirror</name>' >> /root/.m2/settings.xml && \
    echo '      <url>https://maven.aliyun.com/repository/central</url>' >> /root/.m2/settings.xml && \
    echo '    </mirror>' >> /root/.m2/settings.xml && \
    echo '  </mirrors>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 先下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:resolve -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B -T 1C

# 阶段2: 运行阶段（使用阿里云公开镜像，基于 openjdk:8-jre-alpine + 中国时区）
FROM registry.cn-hangzhou.aliyuncs.com/mt23/jre8-alpine-shanghai:1.0.0

# 作者信息
LABEL maintainer="nhq"

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 参数（运行阶段）
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/shops || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
