FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

ARG MODULE=customer-read

WORKDIR /app
COPY . .

RUN if [ -z "$MODULE" ]; then \
      echo "MODULE argument is not set"; \
      exit 1; \
    else \
      echo "Building module: $MODULE"; \
      mvn -B -ntp -Dmaven.test.skip=true -pl "$MODULE" -am package; \
    fi

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG MODULE=customer-read
COPY --from=build /app/${MODULE}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]