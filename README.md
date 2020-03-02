# phoebus 
[![Travis Status](https://travis-ci.org/ControlSystemStudio/phoebus.svg?branch=master)](https://travis-ci.org/ControlSystemStudio/phoebus)
[![Appveyor Status](https://ci.appveyor.com/api/projects/status/kwktt0vf955aged1/branch/master?svg=true)](https://ci.appveyor.com/project/mattclarke/phoebus-o58ne/branch/master)

Phoebus is a framework and a collections of tools to monitor and operate large scale control systems, such as the ones in the accelerator community. Phoebus is an update of the Control System Studio toolset that removes dependencies on Eclipse RCP and SWT.

More information:
https://control-system-studio.readthedocs.io


## Requirements
 - [JDK11 or later, suggested is OpenJDK](http://jdk.java.net/12).
 - [maven 2.x](https://maven.apache.org/) or [ant](http://ant.apache.org/)


## Target Platform

All external dependencies are expected in `dependencies/target/lib`.
They could be obtained by expanding a zip-ed phoebus target from an existing build setup, or via one initial maven run:


```
mvn clean verify -f dependencies/pom.xml
```


## Building with maven

### Quickstart (Build & run)  

To build and run the phoebus product  

```
mvn clean install
cd phoebus-product
mvn exec:java
```

### Building  

To build the entire phoebus stack

```
mvn clean install
```

### Unit Tests

Some unit tests may be sensitive to localization
and fail when executed in a previously untested locale.
Set the environment variable `LANG` to `en_US.UTF-8`
to execute tests in a specific locale,
or build with `mvn -DskipTests ...` to skip tests.

### Running the phoebus application  

To run with maven
```
cd phoebus-product
mvn exec:java
```
To run the product jar
```
cd phoebus-product/target
java -jar product-*-SNAPSHOT.jar
```



## Building with ant

```
ant clean run
```


## Developing with Eclipse

Download Eclipse Oxygen 4.7.1a or later from http://download.eclipse.org/eclipse/downloads/

Start Eclipse like this:

    export JAVA_HOME=/path/to/your/jdk-9-or-later
    export PATH="$JAVA_HOME/bin:$PATH"
    eclipse/eclipse -consoleLog

Check Eclipse Preferences:

 * Java, Installed JREs: JDK 9-or-later should be the default
 * Java, Compiler: JDK Compliance should be "9" or higher


### Use plain Java configuration

Use `File`, `Import`, `General`, `Existing Projects into Workspace`.
Select the phoebus root directory, and check the option to "Search for nested projects".

By default, all projects should be selected ('dependencies', 'core-framework', .., 'product').

The file `dependencies/phoebus-target/.classpath`
needs to be edited to list all the `phoebus-target/target/lib/javafx*.jar` files.

In the Package Explorer, select the `product` project.
Invoke `Run`, `Run Configurations...` from the menu.
In the launch configuration dialog, select `Java Application` and press `New Configuration`.
Note that the project should be pre-set to `product`, and the Dependencies tab should list all the project dependencies of the product,
i.e. all the `core-*` and `app-*` projects.
For a Main class, enter `org.phoebus.product.Launcher`, press `Apply` and then `Run`.


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


## Complete Distribution, including manual and self-update

    # Obtain sources
    git clone https://github.com/ControlSystemStudio/phoebus.git

    # Build the Javadoc, i.e. html files to be included in the manual
    ( cd phoebus/app/display/editor; ant -f javadoc.xml clean all )

    # Building the manual will locate and include
    # all ../phoebus/**/doc/index.rst and ../phoebus/**/doc/html
    ( cd phoebus/docs; make clean html )
    # Windows: Use make.bat html

    # Build Product

    # Fetch dependencies
    ( cd phoebus; mvn clean verify -f dependencies/pom.xml )

    # Create settings.ini for the product with current date
    # and URL of your update site.
    # Update site contains '$(arch)' which client will replace with
    # its host OS (linux, mac, win).
    # Note that this example replaces an existing product/settings.ini.
    # If your product already contains settings.ini,
    # consider using '>>' to append instead of replacing.
    URL='https://controlssoftware.sns.ornl.gov/css_phoebus/nightly/phoebus-$(arch).zip'
    ( cd phoebus;
      app/update/mk_update_settings.sh $URL > phoebus-product/settings.ini
    )

    # Build product & bundle for distribution, including the documentation
    ( cd phoebus; ant clean dist )

    # The files phoebus/phoebus-product/target/*.zip and
    # services/*/target/*.zip can now be distributed,
    # unzipped, launched

Note that the phoebus-product is platform dependent, you get a
`phoebus-0.0.1-linux.zip`, `phoebus-0.0.1-mac.zip` or `phoebus-0.0.1-win.zip`
depending on the build platform.


## Cross-Platform Build

The `dependencies` include the platform-dependent JavaFX library with different content for linux, mac and windows.
When building as described above, the result will be an executable for the build platform.
To build for a different platform, create the `dependencies` in one of these ways:

    # Either create the build platform for Linux..
    ( cd phoebus; mvn clean verify  -Djavafx.platform=linux  -f dependencies/pom.xml )

    # or Mac OS X ..
    ( cd phoebus; mvn clean verify  -Djavafx.platform=mac    -f dependencies/pom.xml )

    # or Windows:
    ( cd phoebus; mvn clean verify  -Djavafx.platform=win    -f dependencies/pom.xml )

The remaining build is the same, for example `ant clean dist` to build the distribution.


## Release

There is a release profile which helps prepare and deploy the a phoebus release.

```
mvn -P release release:prepare
```

- Check that there are no uncommitted changes in the sources
- Check that there are no SNAPSHOT dependencies
- Change the version in the POMs from x-SNAPSHOT to a new version (you will be prompted for the versions to use)
- Transform the SCM information in the POM to include the final destination of the tag
- Run the project tests against the modified POMs to confirm everything is in working order
- Commit the modified POMs
- Tag the code in the SCM with a version name (this will be prompted for)
- Bump the version in the POMs to a new value y-SNAPSHOT (these values will also be prompted for)
- Commit the modified POMs

Additionally:
- Before commiting the changes, there is a script in the target platform `release_classpath.py` which will be executed. This script can be modified to updated the .classpath and other files which need manual intervention during a release.
