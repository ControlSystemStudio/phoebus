Localization
============

We encourage every developer to make their code localizable. The ``NLS`` class (based on the Eclipse RCP `NLS` idea)
has been developed to aid in doing so.

Instead of hard-coding messages (UI strings), the ``NLS`` class allows us to put all a package's messages
into a ResourceBundle called *messages*. This ResourceBundle is a collection of files called ``messages.properties``,
``messages_de.properties``, ``messages_fr.properties``, etc. The ``messages.properties`` file contains the English
localization, ``messages_de.properties`` contains the German localization, and so on. The appropriate messages
(depending on the system locale and the ``user.language`` property) are loaded into the fields of a ``Messages`` class
which we can then use in the UI code.

Creating a localization
-----------------------

To localize Phoebus to your language you need to find every ``messages.properties`` file in the project and create a
translated version called ``messages_xx.properties`` where ``xx`` is the appropriate locale code
(``fr``, ``de``, ``es``, etc.). You should also add your locale to the POM as described in the next section.

Checking the completeness of localizations
------------------------------------------

To ease the maintenance of localizations, a report can be generated to quickly find missing or extra messages
compared to the default English localization.

In order to do so, you must first make sure that the locale you're interested in is listed in the
``configuration/locales`` section of the ``l10n-status`` Maven profile. In the following example, the report will
include the German, Spanish and French localizations.

.. code-block:: xml

    <!-- pom.xml (parent) -->
    <project>
      ...
      <profiles>
        ...
        <profile>
          <id>l10n-status</id>
          <reporting>
            <plugins>
              <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>l10n-maven-plugin</artifactId>
                <version>1.0-alpha-2</version>
                <configuration>
                  <aggregate>true</aggregate>
                  <includes>**/messages*.properties</includes>
                  <locales>
                    <locale>de</locale>
                    <locale>es</locale>
                    <locale>fr</locale>
                  </locales>
                </configuration>
              </plugin>
              ...
            </plugins>
          </reporting>
        </profile>
          ...
      </profiles>
      ...
    </project>

After that, you only need to run

::

    mvn site -P l10n-status

The report will be located at ``target/site/l10n-status.html``.

Writing localizable code
------------------------

Suppose we want to be able to localize the following class:

.. code-block:: java

    package org.phoebus.mypackage;

    public class MyClass
    {
        public void greet()
        {
            System.out.println("Hello");
            System.out.println("How are you today?");
        }
    }

The first step is to create a ``Messages.java`` file with the following boilerplate:

.. code-block:: java

    package org.phoebus.ui.mypackage;

    import org.phoebus.framework.nls.NLS;

    public class Messages
    {
        public static String Hello;
        public static String HowAreYou;

        static
        {
            // initialize resource bundle
            NLS.initializeMessages(Messages.class);
        }

        private Messages()
        {
            // Prevent instantiation.
        }
    }

Then, we replace the hard-coded strings in ``MyClass`` with ``Messages``'s fields:

.. code-block:: java

    package org.phoebus.mypackage;

    import org.phoebus.mypackage.Messages;

    public class MyClass
    {
        public void greet()
        {
            System.out.println(Messages.Hello);
            System.out.println(Messages.HowAreYou);
        }
    }

Finally, we create the *messages* ResourceBundle with all the localizations we want.

messages.properties::

    Hello=Hello
    HowAreYou=How are you doing today?

messages_es.properties::

    Hello=Hola
    HowAreYou=¿Cómo estás hoy?
