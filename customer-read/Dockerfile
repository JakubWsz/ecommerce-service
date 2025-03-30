FROM azul/zulu-openjdk-alpine:21 as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY commons/pom.xml commons/
COPY customer-read/pom.xml customer-read/

RUN ./mvnw dependency:go-offline -B

COPY commons/src commons/src
COPY customer-read/src customer-read/src

RUN ./mvnw clean package -pl customer-read -am -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../customer-read/target/*.jar)

FROM azul/zulu-openjdk-alpine:21-jre
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java", "-cp", "app:app/lib/*", "pl.ecommerce.customer.read.CustomerReadApplication"]