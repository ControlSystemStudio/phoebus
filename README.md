# phoebus
![GitHub Actions Status](https://github.com/ControlSystemStudio/phoebus/actions/workflows/build.yml/badge.svg)

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

Define the JAVA_HOME environment variable to point to your Java installation directory. 
Mac OS users should use something like:
```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.5+10/Contents/Home
```
Verify through:
```
$JAVA_HOME/bin/java -version
```

Make sure your PATH environment variable includes JAVA_HOME and the path to the Maven executable.

### Build

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

To import the project:

* Import Project
* Select the Phoebus directory
* Import project from external model: Maven
* Accept the default options and click Next twice
* Ensure that the JDK is version 11 or above
* Change the project name to Phoebus and click finish

To run the Phoebus application:

* Run | Edit Configurations...
* Select + | Application
* Module: Your JRE 11
* Classpath `-cp`: select `product` from drop-down
* Main class: `org.phoebus.product.Launcher`
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

The Phoebus release process can be used to create tagged releases of Phoebus and publish the Pheobus jars to maven central
using the sonatype repositories.

**Setup**

Create a sonatype account and update the maven settings.xml file with your sonatype credentials

```
  <servers>
   <server>
      <id>phoebus-releases</id>
      <username>shroffk</username>
      <password>*******</password>
   </server>
  </servers>
```

**Prepare the release**  
`mvn release:prepare`  
In this step will ensure there are no uncommitted changes, ensure the versions number are correct, tag the scm, etc.
A full list of checks is documented [here](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html).

**Perform the release**  
`mvn -Pdocs release:perform`  
Checkout the release tag, build, sign and push the build binaries to sonatype. The `docs` profile is needed in order
to create required javadocs jars.

**Publish**  
Open the staging repository in [sonatype](https://s01.oss.sonatype.org/#stagingRepositories) and hit the *publish* button

**Note:**
In order to keep the ant and maven builds in sync, before the prepare:release update the `version` in the 
dependencies\ant_settings.xml to match the release version number. After the release is completed the `version` should 
updated to match the next development snapshotData version.
