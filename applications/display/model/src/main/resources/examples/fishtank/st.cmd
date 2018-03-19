#/bin/sh
#
# Start 'heater' IOC

softIoc -m user=$USER -s -d tank.db -d control.db -d control_diff.db
