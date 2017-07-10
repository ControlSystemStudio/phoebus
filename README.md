# phoebus [![Build Status](https://travis-ci.org/shroffk/phoebus.svg?branch=master)](https://travis-ci.org/shroffk/phoebus)

Phoebus is a framework and a collections of tools to monitor and operate large scale control systems, such as the ones in the accelerator community

More information:
https://docs.google.com/document/d/11W52PRlsRjpIvP81HxUxxR9g180DHDByCohYQ9TQv7U/

## Import into Eclipse
Download Java 9 early access from http://www.oracle.com/technetwork/articles/java/ea-jsp-142245.html

Download Eclipse Oxygen 4.7RC4a from http://download.eclipse.org/eclipse/downloads/

Start Eclipse like this:

	export JAVA_HOME=/path/to/your/jdk-9
	export PATH="$JAVA_HOME/bin:$PATH"
	eclipse/eclipse -consoleLog -vmargs --add-modules=ALL-SYSTEM

Help, Install New Software, "Work With: All Available Sites", filter on market, then install General Purpose Tools/Marketplace Client.

In Help/Eclipse Marketplace, search for Java 9 Support BETA for Oxygen

In Help/Eclipse Marketplace, search for Maven Integration for Eclipse Luna or newer

Use File/Import/Maven/Existing Maven Projects to import the phoebus source code.

There can be a compiler error because the "JRE System Library" in the Package Explorer shows "[J2SE-1.4]".
Right click on the affected projects (greeting-app, probe), Build Path, Configure Build Path, Libraries, Edit the JRE System Library to use the Workspace default (jdk-9).
Restart Eclipse IDE.

Can now start product/src/main/java/org.phoebus.product/Launcher.java.

## Building with maven

### Requirements
 - [JDK9](http://jdk.java.net/9/)
 - [maven 2.x](https://maven.apache.org/)
 
### Building

```
mvn clean install -f core/pom.xml
mvn clean install -f applications/probe/pom.xml
mvn clean install -f applications/logbook/pom.xml
mvn clean install -f applications/greetings/pom.xml
mvn clean install -f phoebus-product/pom.xml
```

### Running test application
```
cd target
java -jar product-0.0.1-SNAPSHOT.jar
```
