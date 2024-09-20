FROM ghcr.io/navikt/baseimages/temurin:21

COPY target/app.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"