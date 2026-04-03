# ---- Etapa 1: Construcción ----
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar archivos del wrapper y pom primero (caching de dependencias)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar el código fuente y compilar
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ---- Etapa 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Usuario no root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
