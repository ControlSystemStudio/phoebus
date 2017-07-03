

CP="phoebus-product/target/product-0.0.1.jar"
CP="$CP:core/framework/target/framework-0.0.1.jar"
CP="$CP:core/core-types/target/core-types-0.0.1.jar"
CP="$CP:core/logging/target/logging-0.0.1.jar"
CP="$CP:applications/greetings/target/greetings-0.0.1.jar"
CP="$CP:applications/probe/target/probe-0.0.1.jar"

for lib in ../dependencies/*.jar
do
  CP="$CP:$lib"
done

java -cp $CP org.phoebus.product.Launcher

