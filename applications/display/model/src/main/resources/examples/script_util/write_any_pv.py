# Example for script that connects to PV,
# writes a value, then disconnects from the PV.
#
# This is usually a bad idea.
# It's better to have widgets connect to PVs,
# 1) More efficient. Widget connects once on start, then remains connected.
#    Widget subscribes to PV updates instead of polling its value.
# 2) Widget will reflect the connection and alarm state of the PV
# 3) Widget will properly disconnect
#
# pvs[0]: PV with name of PV to which to connect
# pvs[1]: PV with value that will be written to the PV
from org.csstudio.display.builder.runtime.script import PVUtil, ScriptUtil

pv_name = PVUtil.getString(pvs[0])
value = PVUtil.getDouble(pvs[1])

print("Should write %g to %s" % (value, pv_name))

try:
    PVUtil.writePV(pv_name, value, 5000)
except:
    ScriptUtil.showErrorDialog(widget, "Error writing %g to %s" % (value, pv_name))

