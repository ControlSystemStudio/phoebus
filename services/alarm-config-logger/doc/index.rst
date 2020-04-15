Alarm Configuration Logging
===========================

A simple service which log all the configuration changes made to the alarm server configuration.

The alarm configuration tree is mapped to a directory structure where each node is represented by a directory and each leaf is a json file describing the alarm configuration for that element.

.. image:: /images/alarm_config_tree.png

The above file structure also uses the git version control system which allows us to trace all changes made to the alarm configuration.

.. image:: /images/git_alarm_config_tree.png
