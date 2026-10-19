# The jar is built by 'mvn verify' in the pipeline (or locally) before the image
FROM eclipse-temurin:25-jre@sha256:dbb43d81099eec91e0800226ed2296d03146ee7857165b95112372ad0d67e87f
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}
COPY target/acceptance-demo.jar /app/app.jar
USER 65532:65532
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
