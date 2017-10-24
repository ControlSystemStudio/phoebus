#executed with jython
""" Input:
    pvs[0] - PV name to use
"""
from connect2j import scriptContext

with scriptContext('widget', 'pvs', PVUtil='util', dict=globals()):
    value = util.getString(pvs[0]);
    # print "update_pv_name.py: Name is %s" % value
    widget.setPropertyValue("pv_name", value)