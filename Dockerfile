# The clinic API, built with the Gradle wrapper on JDK 26 and run on a JRE 26 as a non-root user.
#   docker build -t clinic-api .     (deployment/production/compose.yaml does this)

FROM eclipse-temurin:26.0.2_10-jdk-noble AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# dependencies first, so a code change does not download them again
RUN sh gradlew --no-daemon --quiet dependencies > /dev/null
COPY src src
RUN sh gradlew --no-daemon --quiet bootJar && cp build/libs/cms-*-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:26.0.2_10-jre-noble
RUN useradd --system --uid 10001 clinic
COPY --from=build /app.jar /app/app.jar
USER clinic
EXPOSE 1300
# the API takes at most three quarters of the memory its container is given
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
