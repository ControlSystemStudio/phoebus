Alarm Configuration Logging
===========================

A simple service which logs all the configurationData changes made to the alarm server configurationData.

The alarm configurationData tree is mapped to a directory structure where each node is represented by a directory and each leaf is a json file describing the alarm configurationData for that element.

.. image:: /images/alarm_config_tree.png

The above file structure also uses the git version control system which allows us to trace all changes made to the alarm configurationData.

.. image:: /images/git_alarm_config_tree.png
