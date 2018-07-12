"""
Convenience module for connecting to Java

Author: Amanda Carpenter
"""

import sys
from contextlib import contextmanager
from traceback import format_exc

isPy4J = False
try:
    from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters
    isPy4J = True
except ImportError:
    None #ignore for now

"""
Connect to Java using the given port. (Connect to a GatewayServer listening to the port.)
based on py4j tutorial code at:
    https://www.py4j.org/advanced_topics.html#using-py4j-without-pre-determined-ports-dynamic-port-number
"""
def connectToJava(port):
    if not isPy4J:
        sys.stderr.write("Please install py4j to run scripts in native Python.\n")
    port = int(port)
    if port > 0:
        # connect python side to Java side with Java dynamic port and start python
        # callback server with a dynamic port
        gateway = JavaGateway(
                          gateway_parameters=GatewayParameters(port=port),
                          callback_server_parameters=CallbackServerParameters(port=0))

        # retrieve the port to which the python callback server was bound
        python_port = gateway.get_callback_server().get_listening_port()

        # tell the Java side to connect to the python callback server with the new
        # python port, using the java_gateway_server attribute that retrieves the
        # GatewayServer instance
        addr = gateway.java_gateway_server.getCallbackClient().getAddress()
        gateway.java_gateway_server.resetCallbackClient(addr, python_port)
    else:
        return None
        #raise connect2jException
    return gateway

"""
Ensures the given string keys are in the dict given with keyword argument 'dict',
or, by default, in the module's global dict. Imports classes (for Jython) or
assigns and connects variables to Java (in native Python). The name used in
the given dict can be dictated with keyword arguments; the '=' in key='name'
is analogous to 'as', in that the value obtained (connected or imported) using
the key is placed in a dict entry named 'name'.

In greater detail:
native Python:
Creates a context for the script, where the global variables with the
given names/keyword values are connected to the corresponding Java
objects (provided that those objects were made available in a map
whose keys include the names/keyword keys). Yields a Py4J JavaGateway,
which gives access to static Java classes and methods through the
gateway's jvm attribute. (For details, see
https://www.py4j.org/faq.html#how-to-call-a-static-method.)

Jython:
Imports the class(es) with the given name(s) from the package
org.csstudio.display.builder.runtime.script, and places imported
classes in dict.

The arguments 'widget' and 'pvs' have no effect, and arguments of
the form widget='widgetname' and pvs='pvsname' rely on a dict parameter
that includes 'widget' and 'pvs'; it is recommended to use, for example,
widgetname = widget in the calling module, if these variables absolutely
must be used with a different name.
"""
@contextmanager
def scriptContext(*varnames, **kwargs):
    mydict = globals()
    if 'dict' in kwargs:
        mydict = kwargs['dict']
    if len(sys.argv) > 1: #treat as native Python script
        gateway = None
        try:
            gateway = connectToJava(sys.argv[1])
            map = gateway.getMap()
            for name in varnames:
                try:
                    mydict[name] = map[name]
                except:
                    sys.stderr.write(format_exc())
            for key, val in kwargs.items():
                if key != 'dict':
                    try:
                        mydict[val] = map[key]
                    except:
                        sys.stderr.write(format_exc())
            yield gateway
        finally:
            if gateway != None:
                gateway.shutdown(True)
    else:
        if sys.platform.lower().startswith('java'): #treat as Jython script
            for name in varnames:
                if name != 'widget' and name != 'pvs' and name not in mydict:
                    try:
                        exec("from org.csstudio.display.builder.runtime.script import %s" % name)
                        mydict[name] = vars()[name]
                    except ImportError:
                        sys.stderr.write(format_exc())
            for key, val in kwargs.items():
                if key != 'dict':
                    if key != 'widget' and key != 'pvs':
                        try:
                            exec("from org.csstudio.display.builder.runtime.script import %s as %s" % (key, val))
                            mydict[val] = vars()[val]
                        except ImportError:
                            sys.stderr.write(format_exc())
                    elif key in mydict:
                        mydict[val] = mydict[key]
        else:
            sys.stderr.write("connect2j: script did not meet conditions for known script context\n")
        yield