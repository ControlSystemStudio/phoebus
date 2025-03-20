FROM eclipse-temurin:17-jre

# deployment unit
COPY target/service-alarm-server-*.jar /alarmserver/service-alarm-server-*.jar
COPY target/lib /alarmserver/lib

CMD ["java", "-jar", "/alarmserver/service-alarm-server-*.jar"]
