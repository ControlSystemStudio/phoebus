#!/usr/bin/env python

# ^^ First line must mention "python" ^^
# to instruct display builder to execute script in plain python.

# An external python process will be started for this script.
# It can execute pretty much any python code.
import sys, time
from traceback import format_exc

f = open("/tmp/python.log", "w")
info = "%s,\ninvoked at %s" % (sys.version, time.ctime(time.time()))
f.write(info + "\n")

# The python script could simply exit at this point.

# If the script wants to interact with the display,
# it connects to the display via Py4J.
# The connect2y.py libray of the display builder
# offers a convenient way to do this such that
# almost the same code can be used for Python and Jython.
# In this example, we perform all the steps
# that are usually handled by connect2y
# for illustraition/testing purposes.

try:
    # Fetch Py4Y port used by display from command line
    port = int(sys.argv[1])
    f.write("Invoked with port %d\n" % port)
   
    from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters
    # Connect python side to Java,
    # start python callback server with a dynamic port
    gateway = JavaGateway(gateway_parameters=GatewayParameters(port=port),
                          callback_server_parameters=CallbackServerParameters(port=0))

    # Retrieve the port to which the python callback server was bound
    python_port = gateway.get_callback_server().get_listening_port()

    # Tell the Java side to connect to the python callback server with the new
    # python port, using the java_gateway_server attribute that retrieves the
    # GatewayServer instance
    addr = gateway.java_gateway_server.getCallbackClient().getAddress()
    gateway.java_gateway_server.resetCallbackClient(addr, python_port)
    
    # Fetch proxy objects that allow interaction with display
    map = gateway.getMap()
    for var in map:
        f.write("Received from Java: %s\n" % var)
    # Map should contain at least these:
    widget = map["widget"]
    pvs = map["pvs"]
    PVUtil = map["PVUtil"]
    ScriptUtil = map["ScriptUtil"]
    
    f.write("widget = %s\n" % str(widget))
    f.write("pvs = %s\n" % str(pvs))
    
    # From now on it looks just like the jython example:
    # --------------------------------------------------
    trigger = PVUtil.getInt(pvs[0])
    if trigger:
        try:
            import numpy as np
            info += "\nI have numpy!"
        except:
            info += "\nNo numpy..."
        widget.setPropertyValue("text", info)
    # --------------------------------------------------
	# except when done using the gateway, it needs to be shut down
	# to tell the display that we're done    
    gateway.shutdown(True) 
except:
    f.write(format_exc())

f.close()
