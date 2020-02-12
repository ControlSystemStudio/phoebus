Preference Settings
===================

When you run Phoebus, you may find that it cannot connect to your control system
because for example the EPICS Channel Access address list is not configured.

To locate available preferences, refer to the complete list of
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

  phoebus.sh -settings /path/to/settings.ini

At runtime, you can view the currently effective preference settings
from the menu ``Help``, ``About``. The ``Details`` pane includes a tab
that lists all preference settings in the same format that is used by the
``settings.ini`` file. You can copy settings that you need to change
from the display into your settings file.

The same details pane that lists current preference settings also
offers an ``Import Preferences`` button for loading a ``settings.ini``
file. You may use that as an alternative to the command line ``-settings ..`` option,
but note that settings loaded via this button only become effective
after a restart.

Settings loaded via either the ``-settings ..`` command line option
or the ``Import Preferences`` button are stored in the user location (see :ref:`locations`).
They remain effective until different settings are loaded or the user location is deleted.
It is therefore not neccessary to always run the application with the same
``-settings ..`` command line option. Just invoking with the command line option
once or using the ``Import Preferences`` button once suffices to load settings.
In practice, however, it is advisable to include the ``-settings ..`` command line option
in a site-specific application start script.
This way, new users do not need to remember to once start with the option,
and existing users will benefit from changes to the settings file.

Conceptually, preference settings are meant to hold critical configuration
parameters like the control system network configuration.
They are configured by system administrators, and once they are properly adjusted
for your site, there is usually no need to change them.

Most important, these are not settings that an end user would need to see
and frequently adjust during ordinary use of the application.
For such runtime settings, each applicaition needs to offer user interface options
like context menus or configuration dialogs.

When you package phoebus for distribution at your site, you can also place
a file ``settings.ini`` in the installation location (see :ref:`locations`).
At startup, Phoebus will automatically load the file ``settings.ini``
from the installation location, eliminating the need for your users
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

Load that as the default, then read the ``java.util.prefs.Preferences`` like this::

    package org.phoebus.applications.my_app
    
    import org.phoebus.framework.preferences.PreferencesReader;

    # The class that you pass here determines the package name for your preferences
    final PreferencesReader prefs = new PreferencesReader(getClass(), "/my_app_preferences.properties");
    
    String pref1 = prefs.get("my_setting");
    Boolean pref2 = prefs.getBoolean("my_other_setting");
    // .. use getInt, getDouble as needed

The ``PreferencesReader`` loads defaults from the property file,
then allows overrides via the ``java.util.prefs.Preferences`` API.
By default, the user settings are stored in a ``.phoebus`` folder
in the home directory.
This location can be changed by setting the Java property ``phoebus.user``.

In the future, a preference UI might be added, but as mentioned
the preference settings are not meant to be adjusted by end users.
