FROM ghcr.io/navikt/baseimages/temurin:17-appdynamics
ENV APPD_ENABLED=true

COPY target/app.jar app.jar
COPY export-vault-secrets.sh /init-scripts/export-vault-secrets.sh

ENV JAVA_OPTS="-Xmx1024m \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"