# Requires pvaPy installed and on Python path

from pvaccess import PvObject, STRING, INT, BOOLEAN, DOUBLE, PvaServer
from collections import OrderedDict
from time import sleep

print("")
print("This serves pva://nt_table and pva://table")
print("Try pvinfo <pvname> (example: pvinfo nt_table)")
print("")

#nt_table: complies with NTTable Normative Type
#note: Unless the PvObject's 'values' field is created with an ordered dict, the ordering will be lost.
ntTablePV = PvObject( {'labels' : [STRING],
			'value' : OrderedDict([('id_field', [INT]), ('name_field', [STRING]), ('description_field', [STRING])])},
			'epics:nt/NTTable:1.0')
ntTablePV['labels'] = ['ID', 'Name', 'Description']
#every field under "value" must have the same length (number of elements)
ntTablePV.setStructure({'id_field': [1, 2, 3, 4],
			'name_field' : ["Einstein", "Bohr", "Schroedinger", "Watson & Crick"],
			'description_field' : ["It's All Relative", "Fun Things to Do with Atoms", "Fun Things to Do with Cats", "D.N.A."]})

#table: a table-like pv that contains a "value" which is a structure
#tablePV is not an NTTable because "dingus" is a structure, not a scalar type
#Only the "value" field is displayed; the meta-data field is ignored
dingus_types = OrderedDict([('Name', [STRING]), ('Quality', [DOUBLE])])
value_types = OrderedDict([('OK', [BOOLEAN]), ('Dingus', dingus_types)])
tablePV = PvObject({'meta_data' : STRING, 'value' : value_types})

tablePV['meta_data'] = 'Some example meta-data stuff'
dingus = {'Name' : ["Thing1", "Thing2", "Thing3"], 'Quality' : [90, 50, 75]}
value = {'OK' : [True, False, True], 'Dingus' : dingus}
tablePV.setStructure('value', value)

ntTableStrPV = PvObject({'value' : STRING}, {'value': str(ntTablePV)})
tableStrPV = PvObject({'value' : STRING}, {'value': str(tablePV)})

server = PvaServer()
server.addRecord('nt_table', ntTablePV)
server.addRecord('table', tablePV)
server.addRecord('nt_table_str', ntTableStrPV)
server.addRecord('table_str', tableStrPV)

while(True):
	# a bunch of jiggery-pokery to change the value in a somewhat random-looking way
	value = tablePV['value']

	quality = value['Dingus']['Quality'][1]
	quality -= 20;
	if quality < 0:
		quality = 50
	value['Dingus']['Quality'][1] = quality

	quality = value['Dingus']['Quality'][2]
	quality += 7
	if quality > 100:
		quality = (value['Dingus']['Quality'][1] + quality) / 2
	value['Dingus']['Quality'][2] = round(quality,1)
	
	quality += value['Dingus']['Quality'][0]*15
	quality /= 16
	value['Dingus']['Quality'][0] = round(quality, 1)
	
	tablePV['value'] = value

	tableStrPV['value'] = str(tablePV)

	sleep(1)
