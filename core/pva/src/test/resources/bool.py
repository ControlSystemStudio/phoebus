# Example PVA server for custom boolean (not NT type)
from time import sleep
from pvaccess import PvObject, BOOLEAN, PvaServer


pv = PvObject({'value': BOOLEAN })
server = PvaServer('bool', pv)

print("\nServes 'bool'")

value = True
while True:
    pv['value'] = value
    value = not value
    sleep(1)
