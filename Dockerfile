ARG MODULE=customer-read

FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

ARG MODULE

ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

WORKDIR /app

COPY pom.xml .
COPY commons/pom.xml commons/
COPY ${MODULE}/pom.xml ${MODULE}/

RUN mvn dependency:go-offline -B -pl ${MODULE} -am

COPY commons/ commons/
COPY ${MODULE}/ ${MODULE}/

RUN if [ -z "$MODULE" ]; then \
      echo "ERROR: MODULE argument is not set" && exit 1; \
    else \
      echo "Building module: $MODULE" && \
      ./mvnw -B -ntp -Dmaven.test.skip=true -pl "$MODULE" -am package; \
    fi

FROM eclipse-temurin:21-jre-alpine

ARG MODULE

RUN addgroup -S spring && adduser -S spring -G spring

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE="prod"

WORKDIR /app
RUN chown -R spring:spring /app

USER spring:spring

COPY --from=build --chown=spring:spring /app/${MODULE}/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]