
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

Channel Access settings are configured via :ref:`preference_settings`, most important::
    
    # Channel Access address list
    org.phoebus.pv.ca/addr_list=...
    

PV Access
---------
Process variables that are to be accessed over the PV Access protocol must be identified by a formatted string that
contains the process variable's name.

As PV Access is not the default protocol, process variables accessed over it must have the protocol spectrue;ified with 'pva://'.

The PV Access format is as follows::

    pva://SomePVName
    pva://SomePVName/subfield/subelement

As shown, when accessing structures, the path to a nested structure element can be provided.

PV Access is configured via the following environment variables or Java properties::

    # Address list. When empty, local subnet is used
    export EPICS_PVA_ADDR_LIST = "127.0.0.1  10.1.10.255"

    # Add local broadcast addresses to addr list? (Value YES, NO)
    export EPICS_PVA_AUTO_ADDR_LIST = YES


Simulated
---------
Simulated process variables are useful for tests. They do not communicate with the control system.

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
    * const(value)
    
Examples::

    sim://sine
    sim://ramp
    sim://ramp(1, 10, 0.2)
    sim://noise
    sim://const(42)
    sim://const("Fred")

Local
-----
Local process variables can be used within the application,
for example to send a value from one display to another display within the same application.
They do not communicate with the control system.

Following the "loc://" prefix, the variable name must start with a character A-Z or a-z,
potentially followed by more characters or numbers.
Valid examples are "A", "b", "Example2", "MyVar42".
Invalid examples are "42", "2ndExample".

Next is an optional type selector like "<VLong>" and initial value like "42".
Unless a type selector and initial value are provided, a local value will be of type 'VDouble'
with initial value of 0.

Local process variables only exist as long as they are referenced.
When all references to a local process variable are released, the PV is
deleted.

Examples::

    loc://example
    loc://a_number(42.2)
    loc://an_array(3.8, 2.5, 7.4)
    loc://a_text("Hello")
    loc://large<VLong>(42)
    loc://options<VEnum>(2, "A", "Initial", "B", "C")


Formulas
--------
Formula-based PVs can perform simple computations on constants and other PVs.
The equation can be created via the 'eq://' prefix or alternatively via '='.
Other process variables are referenced via backquotes.

Examples::

    eq://3+4
    =3+4
    =10 + 5*`sim://sine`
    =`sim://ramp`>1 ? 10 : -10


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



