# ==================== 阶段 1：Maven 构建 ====================
FROM maven:3.9.9-eclipse-temurin-21 AS build

# 阿里云 Maven 镜像加速
COPY settings.xml /root/.m2/settings.xml

WORKDIR /build
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B -q || true
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ==================== 阶段 2：JDK 8 运行镜像 ====================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

EXPOSE 9100 9200

CMD ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
