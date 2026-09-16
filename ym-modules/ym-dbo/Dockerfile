FROM bellsoft/liberica-openjdk-rocky:21.0.12-cds

LABEL maintainer="Lion Li"

RUN mkdir -p /ym/dbo/logs /ym/dbo/temp /ym/skywalking/agent

WORKDIR /ym/dbo

ENV SERVER_PORT=9203 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

ADD ./target/ym-dbo.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
