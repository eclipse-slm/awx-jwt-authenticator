FROM openjdk:18.0.2.1-jdk-slim-buster

ENV KEYCLOAK_AUTHSERVERURL="http://keycloak:7080/auth" \
    KEYCLOAK_REALM="fabos" \
    KEYCLOAK_RESOURCE="awx-jwt-authenticator" \
    KEYCLOAK_CREDENTIALS_SECRET="e4d5cd97-be87-447f-b35f-1d30f1b7f887" \
    KEYCLOAK_USERESOURCEROLEMAPPINGS="false" \
    AWX_HOST="awxweb" \
    AWX_SCHEME="http" \
    AWX_PORT=8052 \
    AWX_CLIENTID="" \
    AWX_CLIENTSECRET="" \
    AWX_USERNAME="admin" \
    AWX_PASSWORD="password" \
    AWX_ORGANIZATIONNAME="Service Lifecycle Management" \
    VAULT_SCHEME="http" \
    VAULT_HOST="vault" \
    VAULT_PORT=8200 \
    VAULT_AUTHENTICATION="APPROLE"

RUN apt update && \
    apt install -y curl jq

COPY src/main/resources/truststore/* /app/truststore
COPY target/*-exec.jar /app/app.jar
COPY src/main/docker/startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

WORKDIR /app

ENTRYPOINT ["/bin/bash", "-c", "/app/startup.sh"]
