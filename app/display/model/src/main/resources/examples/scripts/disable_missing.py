# For Phoebus, disable widgets that have not been implemented
# pvs[0] - widget type to check

from org.csstudio.display.builder.runtime.script import ScriptUtil, PVUtil

if 'PHOEBUS' in dir(ScriptUtil):
    from org.csstudio.display.builder.representation.javafx.widgets import BaseWidgetRepresentations

    type = PVUtil.getString(pvs[0])

    supported = False
    for t in BaseWidgetRepresentations().getWidgetRepresentationFactories():
        if t.getType() == type:
            supported = True
            break;
    if not supported:
        print("Disable " + type)
        widget.setPropertyValue("visible", False)
