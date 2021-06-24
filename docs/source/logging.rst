Logging
=======

All phoebus code logs via the ``java.util.logging`` mechanism.

The default log settings for the phoebus product are based on the
``logging.properties`` file of the ``core-launcher`` module.
Services like the alarm server have a similar built-in log configuration file.

To override these built-in settings, create a copy of that file, adjust as desired,
and pass it via the ``-logging /path/to/my_logging.properties`` command line option.

At runtime, the log settings of the product can be adjusted via the "Logging Configuration" application.
The default configuration sends log messages to the console, i.e. the terminal window from which
the product was started.
On Windows, there might not be a terminal, and on other systems, a launcher script might redirect the console output.
The "Error Log" application allows viewing log messages in the product GUI.

