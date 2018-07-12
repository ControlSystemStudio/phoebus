# Requires pvaPy installed and on Python path

# Example PVA server. Serves a table that looks something like this:
# ID	Name	Title
# 1	Fred	Fred1
# 2	Fred	Fred2
# 3	Lisa	Lisa I
# ...	...	...
# A 'name' PV is also served. It can be used to filter table values by name.

# In the past, creating tables from tabular data has involved pickling
# on the server side, then laboriously un-pickling the data and filling
# the table manually on the client side.

# This server simply serves the data, directly, as a table.
# The 'name' PV is monitored so that changes to the name
# affect the values served in the table.

from pvaccess import PvObject, PvString, STRING, UINT, PvaServer, Channel
from collections import OrderedDict
from copy import copy

# table values
titles = ['Fred1', 'Fred2', 'Lisa I', 'Larry One', 'Lisa II', 'Harry One', 'Another Fred', 'Lisa III']
names = ['Fred', 'Fred', 'Lisa', 'Larry', 'Lisa', 'Larry', 'Fred', 'Lisa']
ids = {'Fred' : [1, 2, 7], 'Lisa' : [3, 5, 8], 'Larry' : [4, 6]}

# update value (callback)
def updateValue(pv):
	name = pv['value']
	#print("updating table for name '%s'" % name)
	newPV = PvObject(tableStructDict, copy(tablePV.get()), "epics:nt/NTTable:1.0")
	value = newPV['value']
	if name is '':
		value['id'] = []
		value['name'] = names
		value['title'] = []
		for i in range(0, len(names)):
			value['id'].append(i+1)
			value['title'].append(titles[i])
	elif name in ids:
		value['id'] = ids[name]
		value['name'] = [name] * len(value['id'])
		value['title'] = []
		for id in value['id']:
			value['title'].append(titles[id-1])
	else:
		value['id'] = [0]
		value['name'] = [None]
		value['title'] = [None]
	#print("value = %s" % str(value))
	newPV['value'] = value
	server.update('table', newPV)

# create PVs
tableStructDict = {'labels' : [STRING],
                    'value' : OrderedDict([('id', [UINT]), ('name', [STRING]), ('title', [STRING])])}
tablePV = PvObject(tableStructDict, "epics:nt/NTTable:1.0")
tablePV['labels'] = ['ID', 'Name', 'Title']
namePV = PvString('')

# create server
server = PvaServer('table', tablePV)
server.addRecord('name', namePV, updateValue)

raw_input("\nEnter anything to quit\n")

