# Python code that serves table data via Channel Access
#
# CA does not have a native table data type.
# One option is a string array,
# where the client knows about the number of columns
# and reformates the flat list of cells as rows of columns.
#
# That's used here.
#
# The other option is to create a list(list),
# then pickle into a byte array.
# Since the client is also using python/jython,
# it can un-pickle to get the list(list).
#
# pickle() allows for longer strings and changing
# row and column counts, but the raw data is no
# longer human readable as in the string array case.

from pcaspy import SimpleServer, Driver
import time

prefix = 'Demo:'
pvdb = {
    'Table' : { 'type' : 'string', 'count' : 9 }
}

class MyDriver(Driver):
    def __init__(self):
        super(MyDriver, self).__init__()
        
        self.setParam('Table',
                      [ 'A', 'B', 'C',
                        'a', 'b', 'c',
                        'x', 'y', 'z',
                        'X', 'Y', 'Z'
                      ]
                     )


server = SimpleServer()
server.createPV(prefix, pvdb)
driver = MyDriver()

i = 0
update = time.time() + 5
while True:
    # process CA transactions
    server.process(0.1)
    if time.time() > update:
        i = i + 1
        print("Update %d" % i)
        si = str(i)
        driver.setParam('Table',
                      [ 'A'+si, 'B'+si, 'C'+si,
                        'a'+si, 'b'+si, 'c'+si,
                        'x'+si, 'y'+si, 'z'+si,
                        'X'+si, 'Y'+si, 'Z'+si
                      ]
                     )
        driver.updatePVs()
        update = time.time() + 5
    
