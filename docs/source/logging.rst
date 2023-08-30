Logging
=======

All phoebus code logs via the ``java.util.logging`` mechanism.

The default log settings for the phoebus product are based on the
``logging.properties`` file of the ``core-launcher`` module,
which can be downloaded from https://github.com/ControlSystemStudio/phoebus/blob/master/core/launcher/src/main/resources/logging.properties.
Services like the alarm server have a similar built-in log configuration file,
for instance https://github.com/ControlSystemStudio/phoebus/blob/master/services/alarm-server/src/main/resources/alarm_server_logging.properties.

At runtime, the log settings of the product can be adjusted via the "Logging Configuration" application,
which is most convenient for one-time changes.
To adjust the log settings of the product more permanently, or to adjust the log settings of services
which do not have a GUI, you can use a command line option to override the built-in logging properties.
Create a copy of the file, adjust as desired,
and pass it via the ``-logging /path/to/my_logging.properties`` command line option.

The default configuration sends log messages to the console, i.e. the terminal window from which
the product was started.
On Windows, there might not be a terminal, and on other systems, a launcher script might redirect the console output.
The "Error Log" application allows viewing log messages in the product GUI.

