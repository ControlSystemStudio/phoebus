FROM eclipse-temurin:17-jre

# deployment unit
COPY target/service-scan-server-*-jar-with-dependencies.jar /scanserver/service-scan-server-*-jar-with-dependencies.jar
EXPOSE 4810
CMD ["java", "-jar", "/scanserver/service-scan-server-*-jar-with-dependencies.jar"]
