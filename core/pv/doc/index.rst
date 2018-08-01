
Process Variables
=================

CA
--
Process variables that are to be accessed over the channel access protocol must be identified by a formatted string that
contains the process variable's name.

Channel access is the default protocol so no protocol identification is necessary as can be seen in the other examples.

The Channel Access format is as follows::

    pv_name

where 'pv_name' is replaced with the process variable's actual name.

PVA
---
Process variables that are to be accessed over the PV access protocol must be identified by a formatted string that
contains the process variable's name.

As PV Access is not the default protocol, process variables accessed over it must have the protocol specified with 'pva://'.

The PV Access format is as follows::

    pva://pv_name

where 'pv_name' is replaced with the process variable's actual name.

Sim
---
Simulated process variables must be reference with a formatted string which contains the name of the simulated variable.
The VType should not be included as simulated process variables are predefined within the server and already know what 
types will be dealt with.

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
    
The Simulated format is as follows::

    sim://pv_name

where 'pv_name' is replaced with the simulated process variable's actual name.

Loc
---
Local process variables must be referenced with a formatted string which contains the name of the simulated variable
and the VType that the variable is to have.

Local process variables are instantiated inside the server and are not accessed over a protocol, however to identify them as internal they must be specified with 'loc://'. 

The Local format is as follows::

    loc://pv_name<VType>

where 'pv_name' is replaced with the process variable's actual name and 'VType' is replaced with the VType that the variable is instantiated with.

MQTT
----
Data that is to be read over the MQTT network protocol must be referenced with a formatted string which contains the name of the MQTT topic and the VType that corresponds to the type of data published on the topic.

In core-pv there is a pv_mqtt_preferences.properties file that contains the MQTT broker URL. This should be set to the address of the MQTT broker that the topics will be published to.

As MQTT is not the default protocol, topics accessed over it must have the protocol specified with 'mqtt://'.

The MQTT format is as follows::

    mqtt://topic_name<VType>

where 'topic_name' is replaced with the topic's actual name and 'VType' is replaced with the VType that corresponds to the variable's type.


