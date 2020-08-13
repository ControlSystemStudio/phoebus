Starting CS-Studio/Phoebus
==========================

For build instructions, refer to the README.md on https://github.com/ControlSystemStudio/phoebus

For pre-built binaries, see https://controlssoftware.sns.ornl.gov/css_phoebus/

From the command-line, invoke ``phoebus.sh -help``, which will look
similar to this, but check your copy of CS-Studio/Phoebus
for the complete list::

      _______           _______  _______  ______            _______ 
     (  ____ )|\     /|(  ___  )(  ____ \(  ___ \ |\     /|(  ____ \
     | (    )|| )   ( || (   ) || (    \/| (   ) )| )   ( || (    \/
     | (____)|| (___) || |   | || (__    | (__/ / | |   | || (_____ 
     |  _____)|  ___  || |   | ||  __)   |  __ (  | |   | |(_____  )
     | (      | (   ) || |   | || (      | (  \ \ | |   | |      ) |
     | )      | )   ( || (___) || (____/\| )___) )| (___) |/\____) |
     |/       |/     \|(_______)(_______/|/ \___/ (_______)\_______)
     
     Command-line arguments:
     
     -help                           -  This text
     -settings settings.xml          -  Import settings from file, either exported XML or property file format
     -export_settings settings.xml   -  Export settings to file
     -list                           -  List available application features
     -app probe                      -  Launch an application with input arguments
     -resource  /tmp/example.plt     -  Open an application configuration file with the default application
     -server port                    -  Create instance server on given TCP port
     (..more options, check your actual copy..)


Command Line Parameters for Applications
----------------------------------------

To open an application feature like "probe" or the "pv_tree" from the command line,
use the following example parameters.

Open empty instance of probe::

    phoebus.sh -app probe

Open empty PV Table::

    phoebus.sh -app pv_table

Open a file with the appropriate application feature (PV Table in this case)::

    phoebus.sh -resource "/path/to/example.pvs"

The '-resource' parameter can be a URI for a file or web link::

    phoebus.sh -resource "http://my.site/path/to/example.pvs"

Some resource types are supported by multiple applications.
For example, a display file "my_display.bob" can be handled by both
the "display_runtime" and the "display_editor" application.
A preference setting "org.phoebus.ui/default_apps" defines
which application will be used by default,
and a specific application can be requested like this::

    phoebus.sh -resource "/path/to/my_display.bob?app=display_editor"

The schema 'pv://?PV1&PV2&PV3' is used to pass PV names,
and the 'app=..' query parameter picks a specific app for opening the resource.

Since such resource URLs can contain characters like `&` that would also
be interpreted by the Linux shell, best enclose all resources in quotes.

Open probe with a PV name::

    phoebus.sh -resource "pv://?sim://sine&app=probe"              


Open PV Table with some PVs::

    phoebus.sh -resource "pv://?MyPV&AnotherPV&YetAnotherPV&app=pv_table"              

Note that all these examples use the internal name of the application feature,
for example "pv_table", and not the name that is displayed the user interface,
like "PV Table".
Use the ``-list`` option to see the names of all available application features.

Server Mode
-----------

By default, each invocation of ``phoebus.sh ...`` will start a new instance,
with its own main window etc.

In a control room environment it is often advantageous to run only one instance
on a given computer.
For this scenario, invoke ``phoebus.sh`` with the ``-server`` option, using
a TCP port that you reserve for this use on that computer, for example::

   phoebus.sh -server 4918
   
The first time you start phoebus this way, it will actually open the main window.
Follow-up invocations, for example::

   phoebus.sh -server 4918 -resource "/path/to/some/file.pvs"

will contact the already running instance and have it open the requested file.
