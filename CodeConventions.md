Source Code Conventions
=======================

This code example complies with a set of formatting rules:

```
int m1(final String a) {
    if(a.isEmpty()) {
        return 0;
    }
    int b = 1;
    int c = 0;
    while((c=a.indexOf(':', c)+1)>0) {
        ++b;
    }
    return b;
}
```

To humans, however, the names of classes, methods and variables carry valuable meaning.
Comments and spacing can help explain the code:

```
/** Count path elements
 *  @param path Colon-separated path "/root/folder1:/usr/local:/home/fred"
 *  @return Number of path elements
 */
int countSegments(final String path)
{
    // Empty path has no elements
    if (path.isEmpty())
        return 0;

    // There is at least one path element
    int count = 1;

    // Are there more path elements?
    // Find another separator (or -1),
    // setting `next` to following char (or 0)
    int next = 0;
    while ( (next = path.indexOf(':', next) + 1) > 0)
        ++count;
   
    return count;
}
```

Similarly, alignment can improve readability:

```
// Manually aligned:
if ( (5 < a && a < 10)  ||
     (b > 4 && c < 20)  ||
     (in_override  &&  !override_disabled) )
{
}

// Auto-formatted by simply breaking lines at some column:
if((5<a&&a<10)||(b>4&&c<20)||(in_override&&!
   override_disabled)) {
}
```

We therefore discourage blindly applying an IDE code formatter,
and instead suggest adhering to some key points.


Source Code Format Guidelines
-----------------------------

 * Indent using four (4) spaces, not tabs.
 * Avoid trailing white space at the end of lines.
 * Braces should be 'balanced'
 
```
   // Like this
   class X
   {
      void method()
      {
         if (true)
         {
         }
      }
   }

   // Common, but discouraged
   class X {
      void method() {
         if (true) {
         }
      }
   }

```

 * Public class names use `CamelCase` starting uppercase.
 * Public method names use `camelCase` starting lowercase.
 * Public classes and methods need Javadoc.
   

File Locations
--------------

.. follow general Maven practice:

 * `src/main/java` - Java code for release
 * `src/main/resources` - Resources (`*.fxml`, `*.png`, ...)
 * `src/test/java` - Test code
 * `src/test/resources` - Resources used in the test (example input files, ...)


Unit Tests
----------

All test code under `src/test/java` with classes that end in `*UnitTest.java`
should be such that it can be executed by anybody at any time,
including automated build systems used for continuous test and integration.

Test code that requires manual intervention or can only be executed
within a certain test infrastructure (database, network services, ...)
should be called  `*Demo.java`.


Package Names
-------------

Ideally, all code within one Phoebus core or app 'module' uses
the same package name which is indicative of the module.
For example, the core-ui module uses package `org.phoebus.ui`,
and the PV Tree application uses package `org.phoebus.applications.pvtree`.

Both `org.phoebus` and `org.csstudio` are permitted for a package name root.
The former is preferred for core modules and code that is specific to
the new Phoebus development, while `org.csstudio` is predominantly found
in application modules that have been ported from the Eclipse-based code base,
or want to emphasize that they are not specific to Phoebus.


Logging
-------

Log messages should use the `java.util.logging.Logger`.
Suggested is one logger per module, statically initialized by some key class,
using the package name of the module, to simplify turning the logging for a
module on or off.

```
package org.phoebus.applications.abc;

import java.util.logging.Logger;

/** Some key class for the ABC application
 *  Might be the Phoebus AppDescriptor
 */
class ABCApplication implements AppDescriptor
{
    /** Logger for all ABC code */
    public static final Logger logger = Logger.getLogger(ABCApplication.class.getPackageName());
    ...
```

For larger modules, additional loggers are acceptable, but the extreme of one logger per class is discouraged.


Exceptions
----------

"Throw early, catch late".
Exceptions should be thrown at a low level in the code,
for example when a file cannot be opened,
or some value isn't within an expected range.

At intermediate code levels, there is often no way to properly handle
the exception. Such code must not _hide_ the exception by catching it
and pretending everything is fine.
It might, however, catch the exception and _re-throw_ it with added information:

```
void solveBigProblem(File file) throws Exception
{
    try
    {
       Data data = parseFile(file);
       Analysis ana = analyze(data);
       Solution sol = computeSolution(ana);
       return sol;
    }
    catch (Exception ex)
    {
       // Something failed.
       // Could not open the file?
       // Could not analyze the data?
       // Can't do anything about this right here,
       // but we can add to the information
       // by wrapping the original exception into a new one
       // that mentions what we wanted to do,
       // and what file caused this
       throw new Exception("Cannot solve big problem described in " + file, ex);
    }
}
```

The best way to handle an exception is often at a very high level,
where the proper reaction is to log it, or present it to the user
in a dialog.

```
import static org.phoebus.applications.abc.ABCApplication.logger;

void highLevelCode()
{
    File file = promptUserForFile();
    try
    {
        solveBigProblem(file);
    }
    catch (Exception ex)
    {
        // Bad:
        // `Exception#getMessage()` might be `null` or empty.
        // Missing stack trace
        System.out.println(ex.getMessage());
      
        // Better:
        // Uses logging, shows complete exception detail
        // with message (if there is one),
        // stack trace, nested exception ('cause')
        logger.log(Level.WARNING, "Failed to solve big problem", ex);
      
        // For UI code, there is a helper dialog
        // for showing a message with full exception detail
        ExceptionDetailsErrorDialog.openError(parent_node,
            "Error",
            "Failed to solve big problem",
            ex);
    }
```
