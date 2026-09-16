# 贝尔实验室 Spring 官方推荐镜像 JDK下载地址 https://bell-sw.com/pages/downloads/
FROM bellsoft/liberica-openjdk-rocky:21.0.12-cds
# FROM bellsoft/liberica-openjdk-rocky:25.0.4-cds
# FROM findepi/graalvm:java21-native

LABEL maintainer="Lion Li"

RUN mkdir -p /ym/auth/logs  \
    /ym/auth/temp  \
    /ym/skywalking/agent

WORKDIR /ym/auth

ENV SERVER_PORT=9210 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

ADD ./target/ym-auth.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           #-Dskywalking.agent.service_name=ym-auth \
           #-javaagent:/ym/skywalking/agent/skywalking-agent.jar \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar

