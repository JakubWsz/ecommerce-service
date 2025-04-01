FROM azul/zulu-openjdk-alpine:21 as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY commons/pom.xml commons/
COPY customer-write/pom.xml customer-write/

RUN ./mvnw dependency:go-offline -B

COPY commons/src commons/src
COPY customer-write/src customer-write/src

RUN ./mvnw clean package -pl customer-write -am -DskipTests
RUN mkdir -p /app/logs
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../customer-write/target/*.jar)

FROM azul/zulu-openjdk-alpine:21-jre
VOLUME /tmp
VOLUME ["/app/logs"]
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java", "-cp", "app:app/lib/*", "pl.ecommerce.customer.write.CustomerWriteApplication"]
