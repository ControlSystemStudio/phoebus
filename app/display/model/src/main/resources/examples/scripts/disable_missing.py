# pvs[0] - widget type to check

from org.csstudio.display.builder.runtime.script import PVUtil
from org.csstudio.display.builder.representation.javafx.widgets import BaseWidgetRepresentations

type = PVUtil.getString(pvs[0])

if BaseWidgetRepresentations().getWidgetRepresentationFactories().get(type) is None:
    print("Disable " + type)
    widget.setPropertyValue("visible", False)
