# PVs:
# pvs[0] - PV to enable/disable the trace, e.g. loc://trace0(1)
# pvs[1] - PV for the index of the trace, e.g. loc://index0(0)
from org.csstudio.opibuilder.scriptUtil import PVUtil

enable = PVUtil.getLong(pvs[0]) > 0
index = PVUtil.getLong(pvs[1])

if "getDataBrowserModel" in dir(widget):
    # For BOY code that uses databrowser3
    model = widget.getDataBrowserModel()
else:
    # For BOY code that uses databrowser2
    model = widget.controller.model

traces = model.getItems()
# On first call, model may not have any traces, yet
if traces.size() > index:
	traces.get(index).setVisible(enable > 0)



