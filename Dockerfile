FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY lombok.config .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/target/gitbitex-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 80 7002
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
