FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app
COPY . .

RUN mvn -B -ntp -Dmaven.test.skip=true -pl customer-write -am package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/customer-write/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]