Alarm Logging Service
=====================

The alarm logging service records all alarm messages to create an archive of all 
alarm state changes and the associated actions.

This historical data can be used to:  

1. Discover alarm patterns and trends
2. Generate Statistical reports on alarms
3. Debug the alarm system

.. image:: /images/alarm_kibana.png


*********************
Logged Alarm Messages
*********************

The alarm logging service creates kafka streams which can be configured to monitor one or more alarm topics. All associated with state change or configuration change are filtered, time stamped and added to an elastic index.

Examples:

* **Configuration changes** 

 e.g. when new alarm nodes or pvs are added or removed or existing ones are enabled/disabled 

* **State changes** 

 e.g. alarm state changes from OK to MAJOR

* **Commands** 

 e.g. a user actions to *Acknowledge* an alarm
