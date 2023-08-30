#!/usr/bin/env python
#
# pvaPy-based demo server for custom structure
#
# Test by reading 'pva://pv3', 'pva://pv3/b', 'pva://pv3/b/a'

from time import sleep
from pvaccess import PvObject, INT, PvaServer

inner = {'value': INT }
outer = {'a': inner }
pv = PvObject({'b': outer })
server = PvaServer('pv3', pv)

print("\nServes:")
print("pv3 structure")
print("    structure b")
print("        structure a")
print("            int value 3")

x = 1
while True:
    pv['b.a.value'] = x
    print(x)
    server.update(pv)
    sleep(1)
    x = x + 1


