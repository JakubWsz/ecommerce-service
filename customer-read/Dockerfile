FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

LABEL maintainer="Zespół Deweloperski" \
      description="Mikrousługa odczytu danych klientów"

ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

WORKDIR /app

COPY pom.xml .
COPY commons/pom.xml commons/
COPY customer-read/pom.xml customer-read/
RUN mvn dependency:go-offline -B -pl customer-read -am

COPY commons/ commons/
COPY customer-read/ customer-read/

RUN mvn -B -ntp -Dmaven.test.skip=true -pl customer-read -am package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE="prod"

WORKDIR /app
RUN chown -R spring:spring /app
USER spring:spring

COPY --from=build --chown=spring:spring /app/customer-read/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
