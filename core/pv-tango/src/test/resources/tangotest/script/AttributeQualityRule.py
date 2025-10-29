## Rule: Attribute Quality Rule

from org.csstudio.display.builder.runtime.script import PVUtil
from java import lang
from org.csstudio.display.builder.model.properties import WidgetColor


try:
    pv0 = PVUtil.getDouble(pvs[0])

## Script Body
    if pv0 == 0:
        widget.setPropertyValue('background_color', WidgetColor(0, 255, 0, 255))
    elif pv0 == 1:
        widget.setPropertyValue('background_color', WidgetColor(128, 128, 128, 255))
    elif pv0 == 2:
        widget.setPropertyValue('background_color', WidgetColor(255, 200, 0, 255))
    elif pv0 == 3:
        widget.setPropertyValue('background_color', WidgetColor(0, 255, 0, 255))
    elif pv0 == 4:
        widget.setPropertyValue('background_color', WidgetColor(128, 160, 255, 255))
    elif pv0 == 5:
        widget.setPropertyValue('background_color', WidgetColor(255, 200, 0, 255))
    else:
        widget.setPropertyValue('background_color', WidgetColor(240, 240, 240, 255))

except (Exception, lang.Exception) as e:
    widget.setPropertyValue('background_color', WidgetColor(240, 240, 240, 255))
    if not isinstance(e, PVUtil.PVHasNoValueException):
        raise e
