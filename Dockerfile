# ============================================================
# Flowable Multi-Org Workflow Engine - Dockerfile
# 多阶段构建: Maven 编译 → JRE 运行
# ============================================================

FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -DskipTests -B -q

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

ENV LANG=zh_CN.UTF-8
ENV LC_ALL=zh_CN.UTF-8

WORKDIR /app
COPY --from=builder /build/target/flowable-multi-org-service-*.jar app.jar

RUN addgroup -S flowable && adduser -S flowable -G flowable && \
    mkdir -p /app/logs && chown -R flowable:flowable /app
USER flowable

EXPOSE 9999
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9999/api/wf/org/tree || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xms256m -Xmx1024m} -jar app.jar"]
