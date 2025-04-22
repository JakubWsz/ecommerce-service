FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY ../../pom.xml .
COPY ../../commons ./commons
COPY . ./customer-read
COPY ../../customer-write ./customer-write

RUN mvn -B -ntp -Dmaven.test.skip=true -pl customer-read -am package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/customer-read/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]