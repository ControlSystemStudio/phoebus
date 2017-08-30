mvn clean verify  -f dependencies/pom.xml
# All but first run can use -o for offline
#
mvn -o -DskipTests clean install 
(cd phoebus-product/target; java -jar product-0.0.1-SNAPSHOT.jar  --add-modules=ALL-SYSTEM )
