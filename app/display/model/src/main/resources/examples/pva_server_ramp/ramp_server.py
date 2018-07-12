# Requires pvaPy.
from pvaccess import PvaServer, PvObject, PvInt, PvTimeStamp, PvAlarm
from collections import OrderedDict
import time
import math

timeStamp = PvTimeStamp()
alarm = PvAlarm()

lowAlarmLimit = 1
lowWarningLimit = 3
highWarningLimit = 6
highAlarmLimit = 8
lowAlarmSeverity = 2
lowWarningSeverity = 1
highWarningSeverity = 1
highAlarmSeverity = 2

messages = ["NO_ALARM", "LOLO", "LO", "HI", "HIHI"]

#get an int structure, and add the optional fields timeStamp and alarm
rampStructure = PvInt().getStructureDict()
rampStructure['timeStamp'] = timeStamp.getStructureDict()
rampStructure['alarm'] = alarm.getStructureDict()

#create an int PV with value 0
ramp = PvObject(rampStructure, {'value' : 0}, 'epics:nt/NTScalar:1.0')

server = PvaServer('ramp', ramp)

while True:
	#update ramp value dict instead of ramp, so all values update at once
	rampValue = ramp.get()
	#print(str(rampValue))

	value = rampValue['value']
	rampValue['value'] = (value + 1) % 10

	fraction, secs = math.modf(time.time())
	timeStamp.setSecondsPastEpoch(long(secs))
	timeStamp.setNanoseconds(int(fraction * 1000000000))
	rampValue['timeStamp'] = timeStamp.get()

	alarm['status'], alarm['severity'] = \
		(1, lowAlarmSeverity) if value <= lowAlarmLimit else \
		(2, lowWarningSeverity) if value <= lowWarningLimit else \
		(4, highAlarmSeverity) if value >= highAlarmLimit else \
		(3, highWarningSeverity) if value >= highWarningLimit else \
		(0, 0)
	alarm.setMessage(messages[alarm['status']])
	rampValue['alarm'] = alarm.get()

	ramp.set(rampValue)
	time.sleep(1)
