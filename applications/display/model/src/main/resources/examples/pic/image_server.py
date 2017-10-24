# Example image server
#
# Uses pcaspy to serve a test image

import numpy as np

width = 100
height = 100
img = np.zeros(height * width).reshape(height, width)

# Image value at odd  x or y: 0
# Image value at even x or y: x/2 + y/2 * 50
pixel = 0
for y in range(0, height, 2):
    for x in range(0, width, 2):
        img[y, x] = pixel
        pixel += 1

# Show image (need to close that window to continue)
import matplotlib.pyplot as plt
plt.imshow(img, interpolation='nearest')
plt.title('Raw Pixel IDs for image locations')
plt.show()

# pcaspy-based CA server for image
from pcaspy import SimpleServer, Driver

prefix = 'Test:'
pvdb = { 'TestImage': { 'type': 'int', 'count': width*height } }

class myDriver(Driver):
    def __init__(self):
        super(myDriver, self).__init__()

server = SimpleServer()
server.createPV(prefix, pvdb)
driver = myDriver()
driver.setParam('TestImage', img)

while True:
    server.process(0.1)