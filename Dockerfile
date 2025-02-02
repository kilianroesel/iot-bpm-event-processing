FROM flink
RUN mkdir -p $FLINK_HOME/usrlib
ENV APP_ENVIRONMENT=production
COPY ./target/iot-bpm-event-processing-1.0-SNAPSHOT.jar $FLINK_HOME/usrlib/iot-bpm-event-processing-1.0-SNAPSHOT.jar