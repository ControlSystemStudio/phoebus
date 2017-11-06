# This script is attached to a display
# and triggered by a PV like 
#     loc://initial_trigger$(DID)(1)
# to execute once when the display is loaded.

# Read information for the various devices from somewhere, for example an XML file
import os
import xml.etree.ElementTree as ET
from org.csstudio.display.builder.runtime.script import ScriptUtil

# Locate XML file relative to the display file
display_file = ScriptUtil.workspacePathToSysPath(widget.getDisplayModel().getUserData("_input_file"))
directory = os.path.dirname(display_file)
file = directory + "/devices.xml"

# Parse XML
# Actual content of the XML file would of course depend
# on what's needed to describe one device.
# In here we treat each XML element of a device as a macro
xml = ET.parse(file).getroot()
devices = []
for device in xml.iter("device"):
    macros = dict()
    for el in device:
        macros[el.tag] = el.text
    devices.append(macros)

# For testing, it's tedious to add 200 device entries to the XML file.
# So ignore the XML content and just create 200 elements right here
devices = []
for i in range(200):
    devices.append( { 'NAME' : "Device %d" % (i+1),
                      'PV'   : "sim://noise(-5,5,0.1)"
                    } )

# Create display:
# For each 'device', add one embedded display
# which then links to the template.bob
# with the macros of the device.
from org.csstudio.display.builder.model import WidgetFactory

embedded_width = 165
embedded_height = 25

def createInstance(x, y, macros):
    embedded = WidgetFactory.getInstance().getWidgetDescriptor("embedded").createWidget();
    embedded.setPropertyValue("x", x)
    embedded.setPropertyValue("y", y)
    embedded.setPropertyValue("width", embedded_width)
    embedded.setPropertyValue("height", embedded_height)
    for macro, value in macros.items():
	    embedded.getPropertyValue("macros").add(macro, value)
    embedded.setPropertyValue("file", "template.bob")
    return embedded

display = widget.getDisplayModel()
rows = 35
for i in range(len(devices)):
    x = (i / rows) * embedded_width
    y = 170 + embedded_height*(i % rows)
    instance = createInstance(x, y, devices[i])
    display.runtimeChildren().addChild(instance)
