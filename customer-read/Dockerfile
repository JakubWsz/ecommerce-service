FROM azul/zulu-openjdk-alpine:21 AS build

WORKDIR /app

COPY mvnw .
COPY mvnw.cmd .
RUN chmod +x mvnw
COPY pom.xml .
COPY commons/pom.xml commons/
COPY customer-read/pom.xml customer-read/
COPY customer-read/src customer-read/src

RUN ./mvnw -B -ntp -Dmaven.test.skip=true -pl customer-read -am package

FROM azul/zulu-openjdk-alpine:21-jre

WORKDIR /app

COPY --from=build /app/customer-read/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
