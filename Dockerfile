# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Make sure the wrapper is executable
RUN chmod +x ./mvnw
# Resolve dependencies to speed up subsequent builds
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create upload directory
RUN mkdir -p /opt/app/uploads && chmod 777 /opt/app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
