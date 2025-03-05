FROM eclipse-temurin:17-jre

# deployment unit
COPY target/service-save-and-restore-*.jar /saveandrestore/service-save-and-restore-*.jar

CMD ["java", "-Dauthorization.permitall=false", "-Dauth.impl=demo", "-jar", "/saveandrestore/service-save-and-restore-*.jar"]

