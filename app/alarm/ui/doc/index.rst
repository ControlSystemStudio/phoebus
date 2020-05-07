Alarms
======

Overview
--------

The alarm system monitors the alarm state for a configurable list of PVs.
When the alarm severity of any PV changes from `OK` to for example `MAJOR`,
the alarm system changes to that same alarm severity (transition 1 in the diagram below).

.. image:: images/alarm_states.png
   :width: 80%
   :align: center

For the overall alarm to return to `OK`, two things need to happen:

 * The alarm severity of the PV must return to `OK`
 * The alarm must be acknowledged

Typically, the alarm will persist for a while.
A user acknowledges the alarm (2) and starts to address the underlying issue.
Eventually, the reason for the alarm is removed, the severity of the PV recovers to `OK`,
and the alarm system returns to an overall `OK` state (3).

It is also possible that the underlying issue is short lived, and the 
PV recovers to `OK` on its own.
The alarm system latches the alarm, so users can see that there was an
alarm (4). When the user acknowledges the alarm, the system returns
to an overall `OK` state (5).

The order of PV recovery and acknowledgement does therefore not matter.
There are two more details which are not shown in the diagram.

The alarm system maximizes the alarm severity of a PV.
Assume a PV enters the alarm state (1) because its severity is `MINOR`.
The alarm state will also be `MINOR`. If the PV severity now changes to `MAJOR`,
the alarm state will become `MAJOR` as well. Should the PV severity now return to `MINOR`,
the alarm state will remain `MAJOR` because the alarm system takes note of the highest
PV severity.
As already shown in (4), a PV severity clearing to `OK` still leaves the alarm state
at the highest observed severity until acknowledged.

Finally, while alarms will by default `latch` as described above, an alarm
can be configured to not latch. When such a non-latching PV enters an alarm state (1),
once the PV recovers, it will right away return to `OK` via (4) and (5) without
requiring acknowledgement by an end user.

Note that the alarm system reacts to PVs.
Details of how PVs generate alarms, for example at which threshold
an analog reading would enter a `MINOR` alarm state are determined
in the control system.
The alarm system can notify users of an alarm, but it cannot explain
why the alarm happened and what the user should do.
Each alarm should be configured with at least one "guidance" message
to explain the alarm and a "display" link to a related control system
screen.

Components
----------

The alarm system consists of an alarm server and a user interface.

The Alarm Server monitors a set of PVs, tracking their alarm state.
The alarm server tracks updates to the PVs received from the control system.

The user interface shows the current alarms, allows acknowledgement,
and provides guidance, links to related displays.

Kafka stores the alarm system configuration, and provides the
communication bus between the alarm server and user interface.

.. image:: images/alarm_components.png
   :width: 50%
   :align: center

Refer to `applications/alarm/Readme.md` for setting up Kafka
and the alarm server.


User Interface
--------------

The UI includes the following applications:

 * Alarm Tree: Primarily used to configure the alarm system,
   i.e. to add PVs and define their alarm details.

   The alarm configuration is hierachical,
   starting from for example a top-level `Accelerator`
   configuration to components like `Vacuum`, `RF`,
   with alarm trigger PVs listed below those components.
   Configuration settings for `Guidance`, `Displays` etc.
   are inherited along the hierarchy, so that all alarm under
   `/Accelerator/Vacuum` will see all the guidance and displays
   configured on `Vacuum`.

   The alarm system does not enforce how the hierachical configuration
   is used. The 'components' could be subsystems like `Vacuum`, `RF`,
   or they could refer to areas of the machine like `Front End`,
   `Ring`, `Beam Line`. There can be several levels of sub-components,
   and each site can decide how to arrange their alarm trigger PVs
   to best re-use guidance and display information so that the configuration
   of individual PVs is simplified by benefitting from the inherited
   settings along the hierarchy.

 * Alarm Table: Main runtime interface, shows current alarms.

   Ideally, this table will be empty as the machine is running without issues.
   Once alarms occur, they are listed in a table that users can sort by PV name,
   description, alarm time etc.

   The context menu of selected alarms offers links to guidance messages and
   related displays.  

   Alarms can be acknowledged, which moves them to a separate table of acknowledged
   alarms.

 * Alarm Area Panel: Shows summary of top-level alarm hierarchy components.

   Useful as a basic alarm status indicator that can be checked "across the room".

 * Annunciator: Annunciates alarms.

   Optional component for voice annunciation of new alarms.

Each of the above alarm apps can be launched from the menu.
They can also be opened from the cmd line as follows::

    -resource alarm://localhost/Accelerator?app=alarm_tree
    -resource alarm://localhost/Accelerator?app=alarm_table
    -resource alarm://localhost/Accelerator?app=alarm_area


Guidance
--------

Each alarm should have at least one guidance message to explain the meaning
of an alarm to the user, to list for example contact information for subsystem experts.
Guidance can be configured on each alarm PV, but it can also be configured on
parent components of the alarm hierarchy.

Title
^^^^^

A short title for the guidance that will appear in the context menu of the alarm,
for example "Contacts" or "What to do".


Detail
^^^^^^

A slightly longer text with the content of the guidance, for example a list of
telephone numbers, or description of things to try for handling the alarm.

Displays
--------

As with Guidance, each alarm should have at least one link to a control
system display that shows the actual alarm PV and the surrounding subsystem.

Title
^^^^^

Short title for the display link that will appear in the context menu,
for example "Vacuum Display".

Detail
^^^^^^

The display link.

Examples::

    /path/to/display.bob
    /path/to/display.bob?MACRO=Value&OTHER=42$NAME=Text+with+spaces

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
 
 
