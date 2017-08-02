mvn clean verify  -f dependencies/pom.xml
mvn -o clean install -f core/pom.xml
mvn -o clean install -f applications/probe/pom.xml
mvn -o clean install -f applications/logbook/pom.xml
mvn -o clean install -f applications/greetings/pom.xml
mvn -o clean install -f phoebus-product/pom.xml

(cd phoebus-product/target; java -jar product-0.0.1-SNAPSHOT.jar )
