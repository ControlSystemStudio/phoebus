"""UI for Conway's Game of Life

   Creates widget for each cell of the game.
   Displays updates from running game,
   allows changing the cells.
   
   pvs[0] - dummy PV that once triggers the script.
            Script remains running!!
   pvs[1] - 0/1 PV to pause/run
   
   @author Kay Kasemir
"""
from org.csstudio.display.builder.runtime.script import ScriptUtil, PVUtil
from org.csstudio.display.builder.model import WidgetFactory
from org.csstudio.display.builder.model.properties import WidgetColor
from time import time, sleep
from life import GameOfLife

logger = ScriptUtil.getLogger()
display = widget.getDisplayModel()

SIZE = 19
GAP = 0

def createPiece(row, col, value):
    widget = WidgetFactory.getInstance().getWidgetDescriptor("checkbox").createWidget()
    widget.setPropertyValue("x", col*(SIZE+GAP))
    widget.setPropertyValue("y", 75 + row*(SIZE+GAP))
    widget.setPropertyValue("width", SIZE)
    widget.setPropertyValue("height", SIZE)
    widget.setPropertyValue("label", "")
    widget.setPropertyValue("pv_name", "loc://gol_%d_%d(%d)" % (col, row, value))
    widget.setPropertyValue("tooltip", "Click to set/clear cell")
    return widget

def createMap(map):
    widgets = []
    for row in range(len(map)):
        row_widget = []
        for col in range(len(map[row])):
            widget = createPiece(row, col, map[row][col])
            display.runtimeChildren().addChild(widget)
            row_widget.append(widget)
        widgets.append(row_widget)
    return widgets

def getPVs(widgets):
    map_pvs = []
    for row in range(len(widgets)):
        row_pvs = []
        for col in range(len(widgets[row])):
            pv = ScriptUtil.getPrimaryPV(widgets[row][col])
            if pv is None:
                raise Exception("No PV for row %d, col %d" % (row, col))
            row_pvs.append(pv)
        map_pvs.append(row_pvs)
    return map_pvs

def readMap(map, map_pvs):
    for row in range(len(map)):
        for col in range(len(map[row])):
            map[row][col] = PVUtil.getInt(map_pvs[row][col])

def showMap(map, map_pvs):
    for row in range(len(map)):
        for col in range(len(map[row])):
            map_pvs[row][col].write(map[row][col])

gol = GameOfLife(30, 20)
widgets = createMap(gol.map)
# As widgets are created,each with a PV name,
# the runtime creates those PVs.
# In principle, this script could try to fetch
# the PV of a widget before it has been created.
# One solution would be to wrap this call in a
# try-except, wait a little, then try again.
# In practice, have not seen this happen, yet.
map_pvs = getPVs(widgets)

# Script continues until it's killed when closing the display,
# typically resulting in "KeyboardInterrupt: interrupted sleep"
try:
    while True:
        sleep(0.2)
        readMap(gol.map, map_pvs)
        if PVUtil.getInt(pvs[1]) > 0:
            gol.evolve()
            showMap(gol.map, map_pvs)
except KeyboardInterrupt:
    # Ignore, assume display was closed
    pass
    