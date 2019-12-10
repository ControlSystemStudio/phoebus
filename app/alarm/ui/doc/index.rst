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

Each of the above alarm apps can be launched from the cmd line as follows

``-resource alarm://localhost/Accelerator?app=alarm_tree``

``-resource alarm://localhost/Accelerator?app=alarm_table``

``-resource alarm://localhost/Accelerator?app=alarm_area``


Guidance
--------

Displays
--------

Commands
--------

Automated Actions
-----------------

Automated actions are performed when the node in the alarm hierarchy enters and remains in
an active alarm state for some time.

The intended use case for automated action is to for example send emails
in case operators are currently unable to acknowledge and handle the alarm.
If the alarm should always right away perform some action,
then this is best handled in the IOC.

The automated action configuration has three parts:

Title
^^^^^

The "Title" can be set to a short description of the action.


Delay
^^^^^

The "Delay", in seconds, determines how long the node needs to be in an active alarm state
before the automated action is executed.
A delay of 0 seconds will immediately execute the action, which in practice
suggests that the action should be implemented on an IOC.


Detail
^^^^^^
The "Detail" determines what the automated action will do.


``mailto:user@site.org,another@else.com``:
Sends email with alarm detail to list of recipients.

The email server is configured in the alarm preferences.


``cmd:some_command arg1 arg2``:
Invokes command with list of space-separated arguments.
The special argument "*" will be replaced with a list of alarm PVs and their alarm severity.
The command is executed in the ``command_directory`` provided in the alarm preferences.
 
 
``sevrpv:SomePV``:
Names a PV that will be updated with the severity of the alarm,
i.e. a value from 0 to 9 to represent the acknowledged or active
alarm state.
The delay is ignored for ``sevrpv:`` actions.

Suggested PV template::
 
    # Example for "Severity PV"
    # used with automated action set to "sevrpv:NameOfPV"
    #
    # softIoc -s -m N=NameOfPV -d sevrpv.db
    
    record(mbbi, "$(N)")
    {
        field(ZRVL, 0)
        field(ZRST, "OK")
        field(ONVL, 1)
        field(ONST, "MINOR_ACK")
        field(ONSV, "MINOR")
        field(TWVL, 2)
        field(TWST, "MAJOR_ACK")
        field(TWSV, "MAJOR")
        field(THVL, 3)
        field(THST, "INVALID_ACK")
        field(THSV, "INVALID")
        field(FRVL, 4)
        field(FRST, "UNDEFINED_ACK")
        field(FRSV, "INVALID")
        field(FVVL, 5)
        field(FVST, "MINOR")
        field(FVSV, "MINOR")
        field(SXVL, 6)
        field(SXST, "MAJOR")
        field(SXSV, "MAJOR")
        field(SVVL, 7)
        field(SVST, "INVALID")
        field(SVSV, "INVALID")
        field(EIVL, 8)
        field(EIST, "UNDEFINED")
        field(EISV, "INVALID")
        field(INP,  "0")
        field(PINI, "YES")
    }
 
 