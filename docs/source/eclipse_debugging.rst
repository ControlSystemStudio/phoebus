Eclipse Debugging
=================

Download Eclipse Oxygen 4.7.1a or later from http://download.eclipse.org/eclipse/downloads/

Start Eclipse like this::

   export JAVA_HOME=/path/to/your/jdk-9-or-later
   export PATH="$JAVA_HOME/bin:$PATH"
   eclipse/eclipse -consoleLog

Check Eclipse Preferences::

    Java, Installed JREs: JDK 9-or-later should be the default
    Java, Compiler: JDK Compliance should be "9" or higher

Debugging with Eclipse

This assumes the project has been imported as a maven project into Eclipse(see instructions in README)::

    1. Open Eclipse
    2. Go to `Run->External Tools->External Run COnfigurations`
    3. Create a new `Program` configuration. Set location to `usr/bin/java` on linux.
       This is the location of the Java executable. For any other OS, it should not be too hard
       to find that directory.
    4. Set `Working Directory` to `phoebus/phoebus-product/target`.
    5. Set arguments to:
    ```
    --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true
    -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar path_to_repo/phoebus/phoebus-product/target/product-4.6.6-SNAPSHOT.jar
    ```
    6. Click `Run`. The Eclipse console should output a port number. Write it down; we'll use it for
       debugging later on.
    7. Go to `Debug Configurations`
    8. Create a new `Remote Java Application`
    9. Click on the `Source` tab and make sure all of the sub-modules/projects of the phoebus project
       are checked. This will allow you to travel through source code when debugging code in Eclipse.
    10. For port, add the port from step 6.
    11. Click `Debug`


Now this should connect to your JVM process you started on step 6 and you start debugging your code. Happy debugging!