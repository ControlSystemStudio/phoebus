Alarm Server
============

The alarm server monitors a configurable set of PVs and tracks their alarm state.
When a PV goes into alarm, this is indicated in the alarm system UI.
Operators will usually acknowledge the alarm to indicate that they started
to investigate. They can open guidance information or related displays,
and once the alarm PV recovers, the alarm clears.

For details on the original design based on JMS and an RDB, see
https://accelconf.web.cern.ch/icalepcs2009/papers/tua001.pdf
 
For details on setting up Kafka, see app/alarm/Readme.md 
