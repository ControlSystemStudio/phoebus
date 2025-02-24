FROM eclipse-temurin:17-jre

# deployment unit
COPY target/service-save-and-restore-*.jar /saveandrestore/service-save-and-restore-*.jar

CMD ["java", "-Delasticsearch.network.host=172.18.21.32", "-Dauthorization.permitall=false", "-Dauth.impl=demo", "-DdefaultProtocol=pva", "-jar", "/saveandrestore/service-save-and-restore-*.jar"]
