#executed with jython
""" Input:
    pvs[0] - Value around -5 .. 5
"""
from connect2j import scriptContext

with scriptContext('widget', 'pvs', 'PVUtil', dict=globals()):
    value = PVUtil.getDouble(pvs[0]);
    if value >= 0:
        widget.setPropertyValue("text", "Positive")
    else:
        widget.setPropertyValue("text", "Negative")