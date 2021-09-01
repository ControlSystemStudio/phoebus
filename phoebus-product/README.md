Example Product
===============

This example product selects most of the Phoebus `core-*` and `app-*` components
into an executable UI product.

Each site will likely want to create their own product to

 1. Select which components they need
 2. Add a custom `settings.ini` for their EPICS network settings
 3. Maybe add a custom splash screen, welcome page, launcher
 4. Some sites might want to add site-specific source code

To accomplish 1-3, no source code is needed, only a `pom.xml` or `ant` build file
as shown in this directory.

For site-specific examples, see

 * https://github.com/shroffk/nsls2-phoebus (actually creates several products for 'beamline' vs. 'accelerator')
 * https://github.com/kasemir/phoebus-sns
 
 ## Building native installers with `jpackage`
 
 `jpackage` (https://docs.oracle.com/en/java/javase/14/docs/specs/man/jpackage.html) is a tool bundled with the 
 JDK from version 14. It can be used to build native installers for MacOS (pkg or dmg), 
 Windows (msi or exe) and Linux (deb or rpm). Such installers will include all dependencies, including the Java 
 runtime.
 
The following use cases have been verified:
 
 * MacOS version 10.15.7, dmg and pkg.
 * Windows 10, msi only.
 
 #### Prerequisites

 * A working Phoebus build environment, i.e. JDK 11 and Maven.
 * `jpackage` must be run on the same OS as the target OS, i.e. cross builds are not supported.
 * JDK 14 or newer. 
 * On Windows you also need to install the "WiX" tools, available here: https://wixtoolset.org/.
 * Prepare application icons, to be placed in the top level folder of your product package:
    * `ico` for Windows
    * `icns` for MacOS
    * `png` for Linux
 
 #### Step-by-step
 
 1. Build your product package (zip or gz), e.g. `mvn -Djavafx.platform=[linux|win|mac] clean verify`.
 2. Copy the generated zip/gz file to some temporary folder and cd to it.
 3. Extract the zip/gz. In the following the folder created is referred to as `unzipped`.
 4. Identify the name of the product jar. In the following referred to as `product-<version>-<os>.jar`.
 5. Determine a version for your application, in the following referred to as `app_version`.
 6. For Window installers determine a menu group in which the application will be placed. If the group does not
 exist, it will be created. 
 7. Identify the path to the Java 11 SDK. In the following referred to as `<jdk_root>`. See below for additional
 information on the selecttion of target Java runtime.
 
 ##### `jpackage` build step 1
 `jpackage --name <app_name> --input unzipped --type app-image --main-jar product-<version>-<os>.jar
 --icon unzipped/<OS specific icon file> [--java-options -Dprism.lcdtext=false] --java-options  --java-options
 -Dcom.sun.webkit.useHTTP2Loader=false --runtime-image <jdk_root>`
 
NOTE: the `--java-options -Dprism.lcdtext=false` portion is for MacOS only.
 
Additional Java options are added using `--java-options <my option>`.

 ##### `jpackage` build step 2, MacOS
 `jpackage --name <app_name> --app-version <app_version> --app-image <app_name>.app --type [pkg|dmg]`
 
 ##### `jpackage` build step 2, Windows
 `jpackage --name <app_name> --app-version <app_version> --app-image <app_name> --type [msi|exe] --win-menu --win-shortcut --win-menu-group <menu_group>`

##### `jpackage` build step 2, Linux
 `jpackage --name <app_name> --app-version <app_version> --app-image <app_name> --type [deb|rpm]`
 
#### Additional installer options 
Additional customization of the installer is available for all platforms, 
see https://docs.oracle.com/en/java/javase/14/docs/specs/man/jpackage.html.

### Deployment considerations
The application policy of the target OS may prohibit installation or launch of a package or application downloaded over HTTP(S). On 
Windows 10 the user will be presented with a warning message that can be dismissed to complete the installation. On
MacOS 10.15.7 the installation will complete, but the application may not be able to launch.

Consider either of the following workarounds:
* Copy the installer from a file share. This is apparently considered more safe than HTTP download, and works for
both Windows and MacOS.
* Add a digital signature using a trusted certificate. `jpackage` supports application signing, so it may be incorporated
into the `jpackage` build process. NOTE: this has not been verified.
* Distribute installers - and updates! - using IT management tools. This is the current setup used for Windows
and MacOS at the European Spallation Source.

### Selection of target Java runtime
During build (step 1) a target Java runtime is specified. If this option (`--runtime-image`) is omitted, `jpackage` will
bundle the Java runtime containing the `jpackage` tool, i.e. Java 14+. Tests on Windows shows that the
target runtime selection may impact the end result, i.e. the Phoebus application installed from the msi file. 
For instance, while the Java runtime Adopt JDK 11.0.9 can be bundled into a working installation, 
Adopt JDK 11.0.12 will not work when Phoebus is launched. On MacOS Adopt JDK 11.0.12 works fine.


 
 