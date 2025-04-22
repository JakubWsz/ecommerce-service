FROM azul/zulu-openjdk-alpine:21 AS build

WORKDIR /app

COPY mvnw .
COPY mvnw.cmd .
RUN chmod +x mvnw
COPY pom.xml .
COPY commons/pom.xml commons/
COPY customer-write/pom.xml customer-write/
COPY customer-write/src customer-write/src

RUN ./mvnw -B -ntp -Dmaven.test.skip=true -pl customer-write -am package

FROM azul/zulu-openjdk-alpine:21-jre

WORKDIR /app

COPY --from=build /app/customer-write/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
