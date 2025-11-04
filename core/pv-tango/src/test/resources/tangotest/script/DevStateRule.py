## Rule: DevStateRule

from org.csstudio.display.builder.runtime.script import PVUtil
from java import lang
from org.csstudio.display.builder.model.properties import WidgetColor


try:
    pv0 = PVUtil.getDouble(pvs[0])

## Script Body
    if pv0 == 0:
        widget.setPropertyValue('background_color', WidgetColor(0, 255, 0, 255))
    elif pv0 == 1:
        widget.setPropertyValue('background_color', WidgetColor(255, 255, 255, 255))
    elif pv0 == 2:
        widget.setPropertyValue('background_color', WidgetColor(255, 255, 255, 255))
    elif pv0 == 3:
        widget.setPropertyValue('background_color', WidgetColor(0, 255, 0, 255))
    elif pv0 == 4:
        widget.setPropertyValue('background_color', WidgetColor(255, 255, 255, 255))
    elif pv0 == 5:
        widget.setPropertyValue('background_color', WidgetColor(0, 255, 0, 255))
    elif pv0 == 6:
        widget.setPropertyValue('background_color', WidgetColor(128, 160, 255, 255))
    elif pv0 == 7:
        widget.setPropertyValue('background_color', WidgetColor(255, 255, 0, 255))
    elif pv0 == 8:
        widget.setPropertyValue('background_color', WidgetColor(255, 0, 0, 255))
    elif pv0 == 9:
        widget.setPropertyValue('background_color', WidgetColor(204, 204, 122, 255))
    elif pv0 == 10:
        widget.setPropertyValue('background_color', WidgetColor(128, 160, 255, 255))
    elif pv0 == 11:
        widget.setPropertyValue('background_color', WidgetColor(255, 140, 0, 255))
    elif pv0 == 12:
        widget.setPropertyValue('background_color', WidgetColor(255, 0, 255, 255))
    elif pv0 == 13:
        widget.setPropertyValue('background_color', WidgetColor(155, 155, 155, 255))
    else:
        widget.setPropertyValue('background_color', WidgetColor(240, 240, 240, 255))

except (Exception, lang.Exception) as e:
    widget.setPropertyValue('background_color', WidgetColor(240, 240, 240, 255))
    if not isinstance(e, PVUtil.PVHasNoValueException):
        raise e
