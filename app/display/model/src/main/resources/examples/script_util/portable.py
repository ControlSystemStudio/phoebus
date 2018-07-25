# Porting BOY scripts to the Display Builder
#
# While the underlying widget implementation is very different
# for BOY and the display builder, simple scripts are compatible.
#
# Both tools support
#
#   widget.setPropertyValue("name_of_property", the_value)
#
# with mostly identical property names.
#
# The PVUtil also offers a very similar API,
# only the package name differs.
# 
#   from org.csstudio.opibuilder.scriptUtil import PVUtil
#
# needs to change into
#
#   from org.csstudio.display.builder.runtime.script import PVUtil
#
# This specific package name is actually patched by the
# display builder, so this script, attached to a Label,
# will simply 'run', issuing a warning that the package
# name has been patched:
#
#   from org.csstudio.opibuilder.scriptUtil import PVUtil
#   widget.setPropertyValue("text", "Hello")

# To write a portable script, check for the display builder's widget type:
display_builder = 'getVersion' in dir(widget)

if display_builder:
    from org.csstudio.display.builder.runtime.script import PVUtil, ScriptUtil
    ScriptUtil.getLogger().info("Executing in display builder")
    
    # For the display builder, might further check if running in RCP or Phoebus
    phoebus = 'PHOEBUS' in dir(ScriptUtil)
    if phoebus:
        ScriptUtil.getLogger().info(".. on Phoebus")
    else:
        ScriptUtil.getLogger().info(".. on Eclipse/RCP")

else:
    from org.csstudio.opibuilder.scriptUtil import PVUtil, ConsoleUtil
    ConsoleUtil.writeInfo("Executing in BOY")

# This API is now the same:
val = PVUtil.getDouble(pvs[0])
widget.setPropertyValue("text", "Value is %.3f" % val)

