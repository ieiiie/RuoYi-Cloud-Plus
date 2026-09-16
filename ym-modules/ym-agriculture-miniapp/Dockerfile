FROM bellsoft/liberica-openjdk-rocky:21.0.12-cds
RUN mkdir -p /ym/agriculture-miniapp/logs /ym/agriculture-miniapp/temp /ym/skywalking/agent
WORKDIR /ym/agriculture-miniapp
ENV SERVER_PORT=9223 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""
EXPOSE ${SERVER_PORT}
ADD ./target/ym-agriculture-miniapp.jar ./app.jar
SHELL ["/bin/bash", "-c"]
ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} -jar app.jar
