import sys

from connect2j import connectToJava

if (len(sys.argv) > 1):
    gateway = None
    try:
        gateway = connectToJava(sys.argv[1])
        map = gateway.getMap()
        map['1'] = 1
        gateway.setMap(map)
        map["obj"].setValue("Hello")
    finally:
        if gateway != None:
            gateway.shutdown()