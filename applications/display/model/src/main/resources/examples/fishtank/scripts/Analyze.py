from org.csstudio.display.builder.runtime.script import PVUtil
from org.csstudio.display.builder.model.properties import WidgetColor

pid_out = PVUtil.getDouble(pvs[0])
sine = PVUtil.getDouble(pvs[1])

if pid_out > 600:
    text = "Giving it all ..."
    color = WidgetColor(255, 0, 255) # PINK
elif pid_out > 400:
    text = "Heating a lot ..."
    color = WidgetColor(180, 50, 255) # PURPLE
elif pid_out > 200:
    text = "Heating some ..."
    color = WidgetColor(255, 50, 50) # RED
elif pid_out > 100:
    text = "Warming up ..."
    color = WidgetColor(255, 155, 50) # ORANGE
elif pid_out > 0:
    text = "Keeping warm ..."
    color = WidgetColor(255, 255, 50) # YELLOW
elif pid_out < 0:
    text = "Cooling down ..."
    color = WidgetColor(200, 200, 255) # LIGHT_BLUE 
else:
    text = "Temperature is just right"
    color = WidgetColor(0, 255, 0) # GREEN

widget.setPropertyValue("text", text)
widget.setPropertyValue("background_color", color)
widget.setPropertyValue("x", 440 + sine)
