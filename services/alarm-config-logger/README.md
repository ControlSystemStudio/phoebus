# alarm config logging
Log the configuration changes made to the alarm server configuration

### Dependencies ###

### Running ###

    mvn exec:java

### Description ###

The alarm config model creates a git repository, sharing the same name as the alarm topic, which is used to keep track of the alarm configuration changes.

The repo structure is as follows.

Accelerator/
			.restore-script/config.xml			# It consists of an XMl dump of the alarm server configuration after each config change
			Node1/
				alarmconfig.json 				# A json representation of the alarm configuration of this node
				PV:alarmPV1/
							alarmconfig.json	# A json representation of the alarm configuration of this pv
				PV:alarmPV1/
				PV:alarmPV1/


The split between the config.xml and the file structure is to simplify the process of auditing the changes associated with a single pv of node within the alarm tree. The use of only the version controlled config.xml would require sifting through all the changes on the alarm tree.