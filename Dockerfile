# ---- Derleme ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

# ---- Çalıştırma ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV TZ=Europe/Istanbul
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build /app/target/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-Duser.timezone=Europe/Istanbul","-jar","app.jar"]