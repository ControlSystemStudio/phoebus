# Example image server
#
# Uses pcaspy to serve a test image

import numpy as np
from pcaspy import SimpleServer, Driver, Alarm, Severity
import time

# Image size
width = 4500
height = 1800
# Size of colored boxes
box = 10
# Update Period in seconds
period = 10

def createImage(value):
    value = value % 0xFFFF
    print("Image for %d" % value)
    img = np.ones((height, width), dtype=np.uint16) * value
    
    # Image value at odd  x or y: 0
    # Image value at even x or y: x/2 + y/2 * 50
    pixel = 0
    for y in range(0, height, box):
        for x in range(0, width, box):
            img[y:(y+box/2), x:(x+box/2)] = pixel
            pixel += 1
    print("Range: %d .. %d" % (np.min(img), np.max(img)))
    return img

prefix = 'Test:'
pvdb = { 'TestImage': { 'type': 'short', 'count': width*height } }

class myDriver(Driver):
    def __init__(self):
        super(myDriver, self).__init__()

server = SimpleServer()
server.createPV(prefix, pvdb)
driver = myDriver()

next = 0
while True:
    server.process(0.1)
    now = time.time()
    if now >= next:
        driver.setParam('TestImage', createImage(int(now)))
        driver.setParamStatus('TestImage', Alarm.NO_ALARM,Severity.NO_ALARM)
        driver.updatePVs()
        print("Updated image")
        next = time.time() + period

