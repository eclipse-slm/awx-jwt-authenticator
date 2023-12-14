FROM openjdk:18.0.2.1-jdk-slim-buster

LABEL org.opencontainers.image.source="https://github.com/eclipse-slm/awx-jwt-authenticator"

ENV CONSUL_SCHEME="http" \
    CONSUL_HOST="consul" \
    CONSUL_PORT="8500" \
    CONSUL_ACLTOKEN="your-consul-acl-token"

RUN apt update && \
    apt install -y curl jq

COPY src/main/resources/truststore/* /app/truststore
COPY target/*-exec.jar /app/app.jar
COPY src/main/docker/startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

WORKDIR /app

ENTRYPOINT ["/bin/bash", "-c", "/app/startup.sh"]
