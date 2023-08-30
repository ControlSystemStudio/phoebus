PV Tree
=======

The PV Tree displays the hierarchical data flow between EPICS
records.
It displays the record types and their current values as well as
severity/status.
It attempts to reflect the data flow by traversing input links
(INP, INPA, DOL, ...).

The PV Tree has two modes:

"Run" |run|:
  In this mode it will always display the current value
  of each item in the tree.
 
"Freeze on Alarm" |pause|:
  In this mode, updates pause as soon as the
  root item of the PV Tree goes into alarm.

Usage
-----
Enter a name into the "PV" text box, and see what happens.



Tool Bar Buttons
----------------

|run|, |pause|
  Changes the PV Tree mode between "running" and "freeze on alarm".

|collapse|
  Collapse the tree, i.e. close all sub-sections of the tree.

|alarmtree|
  Display all items in the tree that are in an alarm state.
  Note that this is performed whenever you push the tool bar button.
  If the PV tree items update, branches will not automatically
  show or hide based on their alarm state, because this could
  result in a very nervous display for a rapidly changing
  PV tree.
  Whenever you desire to update the tree to show/hide items,
  push the button.

|tree|
  Expand all sub-sections of the tree.

.. |run| image:: images/icon_run.png
.. |pause| image:: images/icon_pause_on_alarm.png
.. |collapse| image:: images/icon_collapse.gif
.. |alarmtree| image:: images/icon_alarmtree.png
.. |tree| image:: images/icon_pvtree.png


Limitations
-----------

This tool uses the EPICS Channel Access or PV Access network protocols to read PVs.
Note that there is a difference between EPICS records in an IOC and
channels on the network.
There is no way to query EPICS IOCs for their database information
to determine the available "input" links.

Given a PV name ``x``, the PV tree attempts to read the channel ``x.RTYP``.
If the channel is indeed based on a record, it will report the record type.
The knowledge of which links to follow for each record type is
configured into the EPICS PV Tree application via the ``org.phoebus.applications.pvtree/fields``
preference setting.
This allows maintainers of site-specific settings to add support
for locally developed record types, or add certain output links to the
list of links that the PV tree should trace and display.

The Channel Access protocol adds another limitation to the PV tree operation,
because Channel Access strings are restricted to a length of 40 characters.
The PV tree can therefore not read the complete value of links when they exceed
40 characters. This results in long record names being truncated and then failing to
resolve. As a workaround, the PV tree can read a link ``x.INP`` as ``x.INP$`` with a trailing dollar sign,
which causes the IOC to return the value as a byte waveform without length limitations.
This mode, however, is not supported by older IOCs and older CA gateways.
If your site only runs IOCs and gateways that support the ``x.INP$`` channel name syntax,
you can enable the ``org.phoebus.applications.pvtree/read_long_fields=true`` option in the PV tree preferences.
If your site still runs older IOCs, you won't be able to use the PV tree with them unless you
set ``org.phoebus.applications.pvtree/read_long_fields=false``.
