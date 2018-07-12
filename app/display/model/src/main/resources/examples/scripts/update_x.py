#executed with jython
""" Input:
    pvs[0] - Value around -5 .. 5
    pvs[1] - Default value for X
    pvs[2] - Scaling factor
"""
from connect2j import scriptContext

with scriptContext('widget', 'pvs', 'PVUtil', dict=globals()):
	value = PVUtil.getDouble(pvs[0]);
	x0 = PVUtil.getDouble(pvs[1]);
	scale = PVUtil.getDouble(pvs[2]);
	widget.setPropertyValue("x", x0 + scale * value)
