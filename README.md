# phoebus 
[![Travis Status](https://travis-ci.org/shroffk/phoebus.svg?branch=master)](https://travis-ci.org/shroffk/phoebus)
[![Appveyor Status](https://ci.appveyor.com/api/projects/status/kwktt0vf955aged1/branch/master?svg=true)](https://ci.appveyor.com/project/mattclarke/phoebus-o58ne/branch/master)

Phoebus is a framework and a collections of tools to monitor and operate large scale control systems, such as the ones in the accelerator community

More information:
http://phoebus-doc.readthedocs.io


## Requirements
 - [JDK9](http://jdk.java.net/9/)
 - [maven 2.x](https://maven.apache.org/) or [ant](http://ant.apache.org/)


## Target Platform

All external dependencies are expected in `dependencies/target/lib`.
They could be obtained by expanding a zip-ed folder from an existing build setup, or via one initial maven run:


```
mvn clean verify  -f dependencies/pom.xml
```


## Building with maven

 
### Building

```
mvn clean install
```

### Running test application
```
cd phoebus-product/target
java -jar product-0.0.1-SNAPSHOT.jar
```



## Building with ant

```
ant clean run
```


## Developing with Eclipse

Download Eclipse Oxygen 4.7 from http://download.eclipse.org/eclipse/downloads/

Start Eclipse like this:

	export JAVA_HOME=/path/to/your/jdk-9
	export PATH="$JAVA_HOME/bin:$PATH"
	eclipse/eclipse -consoleLog -vmargs --add-modules=ALL-SYSTEM

In Help/Eclipse Marketplace, search for Java 9 Support BETA for Oxygen


### Use plain Java configuration

Use `File`, `Import`, `General`, `Existing Projects into Workspace`.
Select the phoebus root directory.
By default, all projects should be selected ('dependencies', 'core-framework', .., 'product').

Invoke `Run As/Java Application` on the `Launcher` in the product.


### Use Maven Files in Eclipse

In Help/Eclipse Marketplace, search for Maven Integration for Eclipse Luna or newer

Use File/Import/Maven/Existing Maven Projects to import the phoebus source code.

There can be a compiler error because the "JRE System Library" in the Package Explorer shows "[J2SE-1.4]".
Right click on the affected projects (greeting-app, probe), Build Path, Configure Build Path, Libraries, Edit the JRE System Library to use the Workspace default (jdk-9).
Restart Eclipse IDE.

Can now start product/src/main/java/org.phoebus.product/Launcher.java.


## Complete Distribution, including manual

    # Obtain sources for documentation and product
    git clone https://github.com/kasemir/phoebus-doc.git
    git clone https://github.com/shroffk/phoebus.git

    cd phoebus-doc
    # Building the manual will locate and include
    # all ../phoebus/applications/**/doc/index.rst
    make html
    # Windows: Use make.bat html

    cd ../phoebus
    # Fetch dependencies
    mvn clean verify -f dependencies/pom.xml

    # Building the product will bundle
    # ../phoebus-doc/build/html
    # as phoebus-product/target/doc
    ant
    # or mvn clean install
    
    # Could now run the product via phoebus-product/phoebus.sh,
    # or ZIP for distribution
    ant dist

