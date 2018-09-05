# phoebus 
[![Travis Status](https://travis-ci.org/shroffk/phoebus.svg?branch=master)](https://travis-ci.org/shroffk/phoebus)
[![Appveyor Status](https://ci.appveyor.com/api/projects/status/kwktt0vf955aged1/branch/master?svg=true)](https://ci.appveyor.com/project/mattclarke/phoebus-o58ne/branch/master)

Phoebus is a framework and a collections of tools to monitor and operate large scale control systems, such as the ones in the accelerator community. Phoebus is an update of the Control System Studio toolset that removes dependencies on Eclipse RCP and SWT.

More information:
http://phoebus-doc.readthedocs.io


## Requirements
 - [JDK9 or later](http://jdk.java.net/9/)
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

Download Eclipse Oxygen 4.7.1a from http://download.eclipse.org/eclipse/downloads/

Start Eclipse like this:

	export JAVA_HOME=/path/to/your/jdk-9
	export PATH="$JAVA_HOME/bin:$PATH"
	eclipse/eclipse -consoleLog

Check Eclipse Preferences:

 * Java, Installed JREs: JDK 9 should be the default
 * Java, Compiler: JDK Compliance should be "9"


### Use plain Java configuration

Use `File`, `Import`, `General`, `Existing Projects into Workspace`.
Select the phoebus root directory, and check the option to "Seach for nested projects".

By default, all projects should be selected ('dependencies', 'core-framework', .., 'product').

Invoke `Run As/Java Application` on the `Launcher` in the product.


### Use Maven Files in Eclipse

In Help/Eclipse Marketplace, search for Maven Integration for Eclipse Luna or newer

Use File/Import/Maven/Existing Maven Projects to import the phoebus source code.

There can be a compiler error because the "JRE System Library" in the Package Explorer shows "[J2SE-1.4]".
Right click on the affected projects (greeting-app, probe), Build Path, Configure Build Path, Libraries, Edit the JRE System Library to use the Workspace default (jdk-9).
Restart Eclipse IDE.

Can now start product/src/main/java/org.phoebus.product/Launcher.java.


## Developing with Intellij IDEA

First, download the target platform as described above.

To import the project:

* Start Intellij
* Import Project
* Select the Phoebus directory
* Import project from external model: Maven
* Accept the default options and click Next twice
* Ensure that the JDK is version 9 or above
* Change the project name to Phoebus and click finish

To run the Phoebus application:

* Run | Edit Configurations...
* Select + | Application
* Search for main class and type Launcher
* Use classpath of module: select product
* Set the name to Phoebus
* Click OK
* In the top right of the IDE, click the green play button


## Developing with NetBeans

First download [NetBeans 9](https://netbeans.apache.org/download/nb90/nb90.html),
then the target platform as described above. After running NetBeans, select
**Tools** ➜ **Java Platforms** and make sure that a Java 9 or 10 platform is set as
the default one.

To open the Maven project Select the **File** ➜ **Open Project…** and select the
*phoebus* root project folder.

On the **Projects** view right-click on the *phoebus (parent)* node and select the
**Clean and Build** menu item. To build without the unit test, right-click the
*phoebus (parent)* node and select **Run Maven** ➜ **Skip Tests**.

To run the Phoebus application:

 * Open the *phoebus (parent)* project and the *Modules* node, then double-click on
   the  *product* module;
 * Now right-click on the opened *product* project and select *Run*;
 * A dialog will open to select the main class to be run. Verify that
   `org.phoebus.product.Launcher` is selected and press the *Select Main Class*
   button to start the application.
 * You can also select *Remember Permanently* to allow NetBeans remembering the
   chosen class.
 * Right-clicking the *product* project it is also possible to select
   *Set as Main Project*. In this way the Phoebus application can be started just
   pressing the *F6* key, the *Run Main Project* toolbar button, or the
   *Run* ➜ *Run Main Project* menu item.


## Complete Distribution, including manual

    # Obtain sources for Documentation and Product
    git clone https://github.com/kasemir/phoebus-doc.git
    git clone https://github.com/shroffk/phoebus.git

    # Build the Javadoc, i.e. html files to be included in the manual
    ( cd phoebus/app/display/editor; ant -f javadoc.xml clean all )
    
    # Building the manual will locate and include
    # all ../phoebus/**/doc/index.rst and ../phoebus/**/doc/html
	( cd phoebus-doc; make clean html )
    # Windows: Use make.bat html

    # Build Product

    # Fetch dependencies
	( cd phoebus; mvn clean verify -f dependencies/pom.xml )

    # Build product & bundle for distribution, including the documentation
    ( cd phoebus; ant clean dist )
   
    # The files phoebus/*product/target/*.zip can now be distributed,
    # unzipped, launched
