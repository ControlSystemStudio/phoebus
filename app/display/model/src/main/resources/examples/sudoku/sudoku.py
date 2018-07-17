"""
Sudoku board creator.

@author Amanda Carpenter
"""

from org.csstudio.display.builder.runtime.script import PVUtil, ScriptUtil
logger = ScriptUtil.getLogger()

logger.info("Loading sudoku.py")

pvVal = int(PVUtil.getDouble(pvs[0]))
if pvVal == -1:
    board = [ [0,1,2,0,4,5,6,7,8],
    		  [1,2,3,0,5,6,7,8,0],
    		  [2,3,4,0,6,7,8,0,1],
    		  [3,4,5,0,7,8,0,1,2],
    		  [4,5,6,0,8,0,1,2,3],
    		  [5,6,7,0,0,1,2,3,4],
    		  [6,7,8,0,1,2,3,4,5],
    		  [7,8,0,0,2,3,4,5,6],
    		  [8,0,1,0,3,4,5,6,7] ]

elif pvVal == 3:
    board = [ [0,2,0, 0,0,4, 3,0,0],
              [9,0,0, 0,2,0, 0,0,8],
              [0,0,0, 6,0,9, 0,5,0],
              
              [0,0,0, 0,0,0, 0,0,1],
              [0,7,2, 5,0,3, 6,8,0],
              [6,0,0, 0,0,0, 0,0,0],

              [0,8,0, 2,0,5, 0,0,0],
              [1,0,0, 0,9,0, 0,0,3],
              [0,0,9, 8,0,0, 0,6,0] ]
elif pvVal == 2:
    board = [ [5,3,0, 0,7,0, 0,0,0],
              [6,0,0, 1,9,5, 0,0,0],
              [0,9,8, 0,0,0, 0,6,0],
              
              [8,0,0, 0,6,0, 0,0,3],
              [4,0,0, 8,0,3, 0,0,1],
              [7,0,0, 0,2,0, 0,0,6],
              
              [0,6,0, 0,0,0, 2,8,0],
              [0,0,0, 4,1,9, 0,0,5],
              [0,0,0, 0,8,0, 0,7,9] ]
else:
    board = [ [0,0,0, 0,9,0, 4,0,3],
              [0,0,3, 0,1,0, 0,9,6],
              [2,0,0, 6,4,0, 0,0,7],
              
              [4,0,0, 5,0,0, 0,6,0],
              [0,0,1, 0,0,0, 8,0,0],
              [0,6,0, 0,0,1, 0,0,2],
              
              [1,0,0, 0,7,4, 0,0,5],
              [8,2,0, 0,6,0, 7,0,0],
              [7,0,4, 0,5,0, 0,0,0] ]

def getWidget(row, col):
    return display.runtimeChildren().getChildByName("cell%d%d" % (row, col))

def calcPVIndex(row, col):
    return 2+row*9+col

def calcBlockRange(val):
    return range(val-val%3, val-val%3+3)

from org.csstudio.display.builder.model.persist import WidgetColorService
from org.csstudio.display.builder.model.properties import WidgetColor
from org.csstudio.display.builder.model.persist import NamedWidgetColors

display = widget.getDisplayModel()
text = WidgetColorService.getColor(NamedWidgetColors.TEXT)

def createBoard():
    background = display.propBackgroundColor().getValue()
    index = 0
    for row in range(9):
        for col in range(9):
            cell = display.runtimeChildren().getChildByName("cell%d%d" % (row,col))
            value = board[row][col]
            if value != 0:
                cell.setPropertyValue("background_color", background)
                pvs[calcPVIndex(row, col)].write(str(value))
            else:
                pvs[calcPVIndex(row, col)].write(" ")
                cell.setPropertyValue("background_color", cell.propBackgroundColor().getDefaultValue())
            cell.setPropertyValue("foreground_color", text)

pvs[1].write(0) #prevent solving
createBoard()