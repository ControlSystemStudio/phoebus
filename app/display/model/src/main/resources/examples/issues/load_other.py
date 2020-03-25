from org.csstudio.display.builder.runtime.script import ScriptUtil, PVUtil

doit = PVUtil.getInt(pvs[0])
if doit:
    print "Loading other display.."
    ScriptUtil.openDisplay(widget, "other.bob", "REPLACE", None)
    pvs[0].write(0)
