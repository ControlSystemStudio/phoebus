# Script executed by jython

# Can import any Java package
from org.csstudio.display.builder.runtime.script import PVUtil

# Can also import some python code that's available under Jython
import sys, time


trigger = PVUtil.getInt(pvs[0])
if trigger:
    info = "%s,\ninvoked at %s" % (sys.version, time.strftime("%Y-%m-%d %H:%M:%S"))
    widget.setPropertyValue("text", info)
