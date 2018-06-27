Alarms
======

Overview
--------

The alarm system consists of an alarm server and a user interface.

The Alarm Server monitors a set of PVs, tracking their alarm state.

The user interface shows the current alarms, allows acknowledgement,
and provides guidance, links to related displays.

Fundamentally, the alarm server detects when a PV enters an alarm state.
Even if the PV should then leave the alarm state, the alarm server
remembers when the PV first entered the alarm state until the user acknowledges the alarm.


Kafka
-----

Kafka stores the alarm system configuration, and provides the
communication bus between the alarm server and user interface.

Refer to `applications/alarm/Readme.md` for setting up Kafka.


Alarm Server
------------

Run with ``-help`` to see command line options.


User Interface
--------------

Alarm Tree: Allows configuration.

Alarm Table: Main runtime interface, shows current alarms.

Annunciator: Annunciates alarms.

Guidance
--------

Displays
--------

Commands
--------

Automated Actions
-----------------

``mailto:user@site.org,another@else.com``:
Sends email with alarm detail to list of recipients.

The email server is configured in the alarm preferences.


``cmd:some_command arg1 arg2``:
 Invokes command with list of space-separated arguments.
 The special argument "*" will be replaced with a list of alarm PVs and their alarm severity.
 The command is executed in the ``command_directory`` provided in the alarm preferences.
 