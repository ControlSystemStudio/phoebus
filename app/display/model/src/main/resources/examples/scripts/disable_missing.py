# For Phoebus, disable widgets that have not been implemented
# pvs[0] - widget type to check

from org.csstudio.display.builder.runtime.script import ScriptUtil, PVUtil

if 'PHOEBUS' in dir(ScriptUtil):
    from org.csstudio.display.builder.representation.javafx.widgets import BaseWidgetRepresentations

    type = PVUtil.getString(pvs[0])

    if BaseWidgetRepresentations().getWidgetRepresentationFactories().get(type) is None:
        # print("Disable " + type)
        widget.setPropertyValue("visible", False)
