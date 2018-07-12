"""
Writes the value in the widget named 'pv_value'
to the PV named in the widget called 'pv_name'
"""

from org.csstudio.display.builder.runtime.script import PVUtil, ScriptUtil

logger = ScriptUtil.getLogger()

# Locate other widgets in the display based on their name
children = widget.getDisplayModel().runtimeChildren()
name_widget = children.getChildByName("pv_name")
value_widget = children.getChildByName("pv_value")

# Get the current value of those widgets
logger.info("Name widget " + str(name_widget))
logger.info("Value widget " + str(value_widget))
name_pv = ScriptUtil.getPrimaryPV(name_widget)
value_pv = ScriptUtil.getPrimaryPV(value_widget)
name = PVUtil.getString(name_pv)
value = PVUtil.getString(value_pv)

logger.info("Need to set " + name + " = " + value)

# Locate one of the target widgets based on their name
target_widget_name = name.replace("loc://", "")
target_widget = children.getChildByName(target_widget_name)
logger.info("Widget named " + target_widget_name + ": " + str(target_widget))

# Write to the PV of the target widget
pv = ScriptUtil.getPrimaryPV(target_widget)
pv.write(value)
