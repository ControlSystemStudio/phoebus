Preference Settings
===================

When you run Phoebus, you may find that it cannot connect to your control system
because for example the EPICS Channel Access address list is not configured.

To locate available preferences, refer to the complete
:ref:`preference_settings`
or check the source code for files named ``*preferences.properties``,
for example in the ``core-pv`` sources::

   # ----------------------------------------
   # Package org.phoebus.applications.pvtable
   # ----------------------------------------

   # Show a "Description" column that reads xxx.DESC?
   show_description=true

   # -------------------------
   # Package org.phoebus.pv.ca
   # -------------------------

   # Channel Access address list
   addr_list=


Create a file ``settings.ini`` that lists the settings you want to change::

   # Format:
   #
   #  package_name/setting=value
   org.phoebus.pv.ca/addr_list=127.0.0.1 my_ca_gateway.site.org:5066


The ``value`` might be plain text, with details depending on the
preference setting, for example allowing an IP address for the ``addr_list``
or a ``true`` value for some boolean preference setting.
In addition, Java properties or environment variables can be used like this::

   # Example of using a Java property 'gateway'.
   # If it is set to 'my_ca_gateway.site.org:5066',
   # this example would have the same end result as
   # the previous example.
   #
   # If no Java property 'gateway' is found,
   # an environment variable 'gateway' is checked.
   org.phoebus.pv.ca/addr_list=127.0.0.1 $(gateway)


Start Phoebus like this to import the settings from your file::

  phoebus.sh -settings <location>

Where <location> can be specified as either absolute path::

  phoebus.sh -settings /path/to/settings.ini

Or as a file name only::

  phoebus.sh -settings settings.ini

In this case the current directory is checked for presence of the file, then user's home directory.

<location> may also point to a remote URL::

  phoebus.sh -settings http://mysite.com/settings.ini

Loading from URL assumes remote service does not respond with a redirect. Moreover, if using https, the remote URL
must be backed by a trusted certificate.

At runtime, you can view the currently effective preference settings
from the menu ``Help``, ``About``. The ``Details`` pane includes a tab
that lists all preference settings in the same format that is used by the
``settings.ini`` file. You can copy settings that you need to change
from the display into your settings file.

Settings loaded via the ``-settings ..`` command line option
only remain effective while the application is running.
It is therefore necessary to always run the application with the same
``-settings ..`` command line option to get the same results.
In practice, it is advisable to include the ``-settings ..`` command line option
in a site-specific application start script or add them to a site-specific
product as detailed below.
This way, new users do not need to remember any command line settings
because they are applied in the launcher script or bundled into the product.

Conceptually, preference settings are meant to hold critical configuration
parameters like the control system network configuration.
They are configured by system administrators, and once they are properly adjusted
for your site, there is usually no need to change them.

Most important, these are not settings that an end user would need to see
and frequently adjust during ordinary use of the application.
For such runtime settings, each application needs to offer user interface options
like context menus or configuration dialogs.

When you package phoebus for distribution at your site, you can also place
a file ``settings.ini`` in the installation location (see :ref:`locations`).
At startup, Phoebus will automatically load the file ``settings.ini``
from the installation location, eliminating the need for your users or a launcher script
to add the ``-settings ..`` command line option.


.. _preferences-notes:

Developer Notes
---------------

In your code, create a file with a name that ends in ``*preferences.properties``.
In that file, list the available settings, with explanatory comments::

   # ---------------------------------------
   # Package org.phoebus.applications.my_app
   # ---------------------------------------

   # Note that the above
   #
   #    "# Package name.of.your.package"
   #
   # is important. It is used to format the online help,
   # and users will need to know the package name to
   # assemble their settings file.

   # Explain what each setting means,
   # what values are allowed etc.
   my_setting=SomeValue

   # Enable some feature, allowed values are true or false
   my_other_setting=true

In your application code, you can most conveniently access them like this::

    package org.phoebus.applications.my_app

    import org.phoebus.framework.preferences.AnnotatedPreferences;
    import org.phoebus.framework.preferences.Preference;

    class MyAppSettings
    {
        @Preference public static String my_setting;
        @Preference public static boolean my_other_setting;

        static
        {
            AnnotatedPreferences.initialize(MyAppSettings.class, "/my_app_preferences.properties");
        }
    }


The ``AnnotatedPreferences`` helper will read your ``*preferences.properties``,
apply updates from ``java.util.prefs.Preferences`` that have been added via ``-settings ..``, and then set the values
of all static fields annotated with ``@Preference``.
It handles basic types like ``int``, ``long``, ``double``, ``boolean``, ``String``,
``File``. It can also parse comma-separated items into ``int[]`` or ``String[]``.

By default, it uses the name of the field as the name of the preference setting,
which can be overridden via ``@Preference(name="name_of_settings")``.
If more elaborate settings need to be handled, ``AnnotatedPreferences.initialize``
returns a ``PreferencesReader``, or you could directly use that lower level API like this::

    package org.phoebus.applications.my_app

    import org.phoebus.framework.preferences.PreferencesReader;

    # The class that you pass here determines the package name for your preferences
    final PreferencesReader prefs = new PreferencesReader(getClass(), "/my_app_preferences.properties");

    String pref1 = prefs.get("my_setting");
    Boolean pref2 = prefs.getBoolean("my_other_setting");
    // .. use getInt, getDouble as needed.
    // For more complex settings, use `get()` to fetch the string
    // and parse as desired.

The ``PreferencesReader`` loads defaults from the property file,
then allows overrides via the ``java.util.prefs.Preferences`` API
that is used when loading a ``settings.ini`` in the installation location
and by the ``-settings ..`` provided on the command line.
