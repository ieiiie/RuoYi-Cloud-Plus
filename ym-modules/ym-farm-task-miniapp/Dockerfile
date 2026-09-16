FROM bellsoft/liberica-openjdk-rocky:21.0.12-cds
RUN mkdir -p /ym/farm-task-miniapp/logs /ym/farm-task-miniapp/temp /ym/skywalking/agent
WORKDIR /ym/farm-task-miniapp
ENV SERVER_PORT=9224 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""
EXPOSE ${SERVER_PORT}
ADD ./target/ym-farm-task-miniapp.jar ./app.jar
SHELL ["/bin/bash", "-c"]
ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} -jar app.jar
