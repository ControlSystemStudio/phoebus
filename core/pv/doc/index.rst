
Process Variables
=================

Several types of process variables are supported.
A prefix of the form "xx://.." is typically used to select the PV type.

Channel Access
--------------
Process variables that are to be accessed over the channel access protocol are simply identified by
the channel name.

Channel access is the default protocol.
If desired, 'ca://' can be used to specifically select channel access,
but for the time being no protocol identification is necessary for channel access.

Examples::

    SomePVName
    ca://SomePVName
    SomeMotor.RBV



PV Access
---------
Process variables that are to be accessed over the PV Access protocol must be identified by a formatted string that
contains the process variable's name.

As PV Access is not the default protocol, process variables accessed over it must have the protocol specified with 'pva://'.

The PV Access format is as follows::

    pva://SomePVName
    pva://SomePVName/subfield/subelement

As shown, when accessing structures, the path to a nested structure element can be provided.

Simulated
---------
Simulated process variables are ueful for tests. They do not communicate with the control system.

The provided simulated process variables are:
    * flipflop(update_seconds)
    * gaussianNoise(center, std_dev, update_seconds)
    * gaussianWave(period, std_dev, size, update_seconds)
    * intermittent(update_seconds)
    * noise(min, max, update_seconds)
    * noisewave(min, max, update_seconds)
    * ramp(min, max, update_seconds)
    * sawtooth(period_seconds, wavelength, size, update_seconds)
    * sine(min, max, update_seconds)
    * sinewave(period_seconds, wavelength, size, update_seconds)
    * strings(update_seconds)
    
Examples::

    sim://sine
    sim://ramp
    sim://ramp(1, 10, 0.2)
    sim://noise

Local
-----
Local process variables can be used within the application,
for example to send a value from one display to another display within the same application.
They do not communicate with the control system.

Unless a type selector and initial value are provided, a local value will be of type 'double'
with initial value of 0.

Examples::

    loc://example
    loc://a_number(42.2)
    loc://an_array(3.8, 2.5, 7.4)
    loc://a_text("Hello")
    loc://large<VLong>(42)
    loc://options<VEnum>(2, "A", "Initial", "B", "C")


MQTT
----
Data that is to be read over the MQTT network protocol must be referenced with a formatted string
which contains the name of the MQTT topic and the VType that corresponds to the type of data published on the topic.

All MQTT topics are obtained from the same MQTT broker URL, based on a preference setting that defaults to::

    org.phoebus.pv.mqtt/mqtt_broker=tcp://localhost:1883

If the VType is omitted, 'double' is assumed. Examples::

    mqtt://some_topic
    mqtt://some_topic<VDouble>
    mqtt://some_topic<VString>
    mqtt://some/nested/topic



