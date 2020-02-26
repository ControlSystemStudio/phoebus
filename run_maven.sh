#!/bin/bash
# To include Oracle:
# mkdir -p dependencies/install-jars/lib/ojdbc
# cp /path/to/ojdbc8-12.2.0.1.jar dependencies/install-jars/lib/ojdbc
mvn clean verify  -f dependencies/pom.xml
# All but first run can use -o for offline
#
mvn -DskipTests clean install
(cd phoebus-product/target; java -jar product-*-SNAPSHOT.jar  --add-modules=ALL-SYSTEM )

# Or:
# (cd scan-server-product/target; java -jar scan-server-product-0.0.1-SNAPSHOT.jar )
# (cd alarm-server-product/target; java -jar alarm-server-product-0.0.1-SNAPSHOT.jar )
# (cd services/archive-engine/target; java -jar service-archive-engine-0.0.1-SNAPSHOT.jar -help )
