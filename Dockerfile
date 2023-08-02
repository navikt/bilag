FROM ghcr.io/navikt/baseimages/temurin:17

COPY app/target/app.jar /app/app.jar

ENV JAVA_OPTS="-Xmx1024m \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"