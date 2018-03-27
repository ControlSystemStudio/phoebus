# To include Oracle:
# export ORACLE_JDBC_JAR=/path/to/ojdbc8-12.2.0.1.jar 
mvn clean verify  -f dependencies/pom.xml
# All but first run can use -o for offline
#
mvn -DskipTests clean install 
(cd phoebus-product/target; java -jar product-0.0.1-SNAPSHOT.jar  --add-modules=ALL-SYSTEM )

# Or:
# (cd scanserver-product/target; java -jar scan-server-product-0.0.1-SNAPSHOT.jar )
