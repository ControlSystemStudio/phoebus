Dependencies
============

While the Phoebus framework tries to minimize unnecessary dependencies,
both the framework and the CS-Studio tools built on it naturally have external dependencies.
Based on our experience with the previous, Eclipse-RCP-based version of CS-Studio,
we set two goals:

 * Establish a well-defined, known, reproducible target platform
 * Support multiple build tools

Target Platform
---------------

The Target Platform is basically a set of `*.jar` files or Java modules
that provides all the necessary external dependencies,
including for example a JSON and a MySQL client library.

 * The content of the target platform, i.e. the exact set of files,
   is well known.
   For example, it would specify "jackson-core-2.9.4.jar"
   and "mysql-connector-java-5.1.45.jar",
   i.e. the exact name and version of the JSON and MySQL libraries.
 * The target platform is establisted early on in the build process.
   It may be assembled by the first step in the build process,
   it may be downloaded as a ZIP file.
   The point is that the complete target platform is available
   before the first source file gets compiled.
 * The target platform allows offline development.
   Given the target platform and the CS-Studio source files,
   no network access is needed because all sources and their
   external dependencies are available.
 * The target platform is in a format that allows using
   different build tools
   
Our current way of defining and creating the target platform
is Maven-centric, but with slight adjustments and
conventions over a Maven-only approach to meet all goals.
Instead of depending on Maven for the complete build process,
using Maven to only build the `dependencies` will assemble
all external dependencies in `dependencies/phoebus-target/target/lib`.

From then on, any build tool can be used;
no additional network access is required to download more dependencies.

As an alternative to using Maven, the `target/lib` files could also
be copied from another build setup,
including the snapshot from a nightly build server.
It could even be collected manually based on the
information in the `dependencies/**/pom.xml` files.

Build Tools
-----------

In principle, once the target platform has been assembled,
any of the build tools can be used to develop, built and run CS-Studio.
It is also possible to switch between built tools.

Building the target platform, however, is easiest using Maven.

### Maven
Each set of Phoebus framework, CS-Studio core, application and service source files
comes with a Maven `pom.xml` that lists its dependencies.
Maven can automatically download these dependencies.
Without actually listing the complete target platform content,
Maven determines it each time the sources are compiled.
Such a pure Maven approach is in fact quite convenient while it works,
but can become very troublesome in case of build errors,
especially when combined with limited network access:

 * The dependency information in the `pom.xml` is specific to Maven.
 * If one `pom.xml` lists "ab5.jar" as a dependency,
   and another lists "ab9.jar", which version will be used?
 * The complete target platform is only known after successfully(!)
   building everything.

To overcome these issues, we list all external dependencies
that can be obtailed from Maven-Central in the file `dependencies/phoebus-target/pom.xml`.
The `dependencies/install-jars` subtree allows adding dependencies that cannot be downloaded.

One initial maven build of the dependencies collects the target platform into
`dependencies/phoebus-target/target/lib`:

```
mvn clean verify -f dependencies/pom.xml
```

To truly use the files from the target platform,
all the other `pom.xml` could now refer to them via 'system' references:
```
<dependency>
  <groupId>some.group</groupId>
  <artifactId>whatever</artifactId>
  <version>2.0</version>
  <scope>system</scope>
  <systemPath>../../dependencies/phoebus-target/target/lib/whatever2.0.jar</systemPath>
</dependency>
```

In reality we keep the normal pom file dependency references without `systemPath`,
but we only use references that are also listed in the `dependencies/phoebus-target/pom.xml`.
If a submodule `pom.xml` needs an added dependency, or one with a new version number,
the target platform `pom.xml` must be updated as well.
This way, the initial build of the `dependencies` module will fetch all dependencies,
and subsequent Maven builds of other modules will be able to use cached copies.

### Ant
Ant builds use the target platform files from `dependencies/phoebus-target/target/lib`.
The target platform thus needs to be created as decribed for Maven,
or you obtain a copy of such a `target/lib` from a previous build.

### Eclipse
Eclipse `.project` and `.classpath` files are included which refer to the
platform files.

### Intellij IDEA; NetBeans
Uses the Maven `pom.xml`, so once the `dependencies` have been built,
all dependencies should be available.
