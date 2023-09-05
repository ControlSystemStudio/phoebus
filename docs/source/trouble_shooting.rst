Troubleshooting
===============

.. contents:: Here is a non exhaustive list of issues and the corresponding procedures to fix them.

Slow running and latency behavior
---------------------------------
**symptoms**

| Phoebus is slow or nearly freezing when there are a lot of views opened
| or when a view is connected to a lot of process variables.

**procedure**

| Increase the Java Heap Size allocation. It works for any Java Application (Eclipse, CS-Studio ...)
| Edit launching scripts phoebus.sh or phoebus.bat
| and configure JVM options Xms and Xmx (Java Heap Minimum Size  and Java Heap Maximum Size) 

.. code-block:: shell

  java -Xms2048m -Xmx2048m


Impossible to run Phoebus under linux
-------------------------------------
**symptoms**

| If you get the following error message:

.. code-block:: kmsg
  
  /bin/java : permission denied

| or

.. code-block:: kmsg

  Error initializing QuantumRenderer: no suitable pipeline found
  java.lang.RuntimeException: java.lang.RuntimeException: Error initializing QuantumRenderer: no suitable pipeline found
    at com.sun.javafx.tk.quantum.QuantumRenderer.getInstance(QuantumRenderer.java:283)
    at com.sun.javafx.tk.quantum.QuantumToolkit.init(QuantumToolkit.java:253)
    at com.sun.javafx.tk.Toolkit.getToolkit(Toolkit.java:268)
    at com.sun.javafx.application.PlatformImpl.startup(PlatformImpl.java:291)
    at com.sun.javafx.application.PlatformImpl.startup(PlatformImpl.java:163)
    at com.sun.javafx.application.LauncherImpl.startToolkit(LauncherImpl.java:659)
    at com.sun.javafx.application.LauncherImpl.launchApplication1(LauncherImpl.java:679)
    at com.sun.javafx.application.LauncherImpl.lambda$launchApplication$2(LauncherImpl.java:196)
    at java.base/java.lang.Thread.run(Thread.java:831)
  Caused by: java.lang.RuntimeException: Error initializing QuantumRenderer: no suitable pipeline found
    at com.sun.javafx.tk.quantum.QuantumRenderer$PipelineRunnable.init(QuantumRenderer.java:95)
    at com.sun.javafx.tk.quantum.QuantumRenderer$PipelineRunnable.run(QuantumRenderer.java:125)
    ... 1 more
  Exception in thread "main" java.lang.RuntimeException: No toolkit found
    at com.sun.javafx.tk.Toolkit.getToolkit(Toolkit.java:280)
    at com.sun.javafx.application.PlatformImpl.startup(PlatformImpl.java:291)
    at com.sun.javafx.application.PlatformImpl.startup(PlatformImpl.java:163)
    at com.sun.javafx.application.LauncherImpl.startToolkit(LauncherImpl.java:659)
    at com.sun.javafx.application.LauncherImpl.launchApplication1(LauncherImpl.java:679)
    at com.sun.javafx.application.LauncherImpl.lambda$launchApplication$2(LauncherImpl.java:196)
    at java.base/java.lang.Thread.run(Thread.java:831)


**procedure**

| Change the jdk and javafx folders rights :
| *chmod -R 755 Phoebus_install*


Choose OS target when building Phoebus
--------------------------------------
**symptoms**

| Phoebus is built on an OS (Linux for instance) but will run on another one (Windows for instance)

**procedure**

| It is possible to do a cross-build by specifying -Djavafx.platform=<os> on the Maven command line, where os is linux, win or mac.


Cannot modify Phoebus layout anymore
------------------------------------
**symptoms**

| Impossible to close some views or replace them, even after restarting Phoebus.

**procedure**

| All the view settings are stored in a file named memento.
| To reset all the settings, you must delete this file :

* under linux : /home/user/.phoebus/memento
* under windows : C:\\users\\.phoebus\\memento

Start alarm services without the console
----------------------------------------
**symptoms**

| Phoebus Alarm Server or Phoebus Alarm Logger starts with a console.

**procedure**

| The services can also be started without any prompt.
| Start the service with *-noshell* argument 

.. code-block:: systemd
  
  #Phoebus alarm server
  ExecStart=/opt/alarm-phoebus-server/current/alarm-server.sh -settings ${SERVER}/settings.ini -config ${CONFIG} -noshell

.. code-block:: systemd
  
  #Phoebus alarm logger
  ExecStart=/opt/alarm-logger/current/alarm-logger.sh -properties ./application.properties -noshell


No PV found by Alarm Server
---------------------------
**symptoms**

Phoebus Alarm Server does not find any PV.

**procedure**

| Phoebus Alarm Server doesn't use the environment variable EPICS_CA_ADDR_LIST.
| It uses the parameter org.phoebus.pv.ca/addr_list in settings.ini to find the Channel Access list.
| The path to the settings.ini can be given by the --settings argument

.. code-block:: systemd
  
  ExecStart=/opt/alarm-phoebus-server/current/alarm-server.sh -settings ${SERVER}/settings.ini -config ${CONFIG} -noshell

