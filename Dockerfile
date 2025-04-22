ARG MODULE=customer-read

FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

ARG MODULE

WORKDIR /app
COPY . .

RUN if [ -z "$MODULE" ]; then \
      echo "ERROR: MODULE argument is not set" && exit 1; \
    else \
      echo "Building module: $MODULE" && \
      mvn -B -ntp -Dmaven.test.skip=true -pl "$MODULE" -am package; \
    fi

FROM eclipse-temurin:21-jre-alpine

ARG MODULE

WORKDIR /app
COPY --from=build /app/${MODULE}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]