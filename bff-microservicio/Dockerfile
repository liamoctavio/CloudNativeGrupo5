# Build
FROM maven:3.9.8-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -e -B -DskipTests package

# Runtime
FROM eclipse-temurin:11-jre
ENV JAVA_OPTS=""
WORKDIR /opt/app
COPY --from=build /app/target/bff-spring-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
