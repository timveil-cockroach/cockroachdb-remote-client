FROM maven:3-eclipse-temurin-24 AS builder
WORKDIR /app
COPY ./pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./src ./src
RUN mvn package && cp target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM cockroachdb/cockroach:latest AS cockroach

FROM eclipse-temurin:21-jdk

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create app directory and set ownership
WORKDIR /app
RUN chown -R appuser:appuser /app

# Copy application files
COPY --from=builder --chown=appuser:appuser /app/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /app/application/ ./

# Copy cockroach binary and make it executable for the user
COPY --from=cockroach --chown=appuser:appuser /cockroach/cockroach /usr/local/bin/cockroach
RUN chmod +x /usr/local/bin/cockroach

# Switch to non-root user
USER appuser

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]