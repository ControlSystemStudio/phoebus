# Script that receives a n image widget's "Cursor Info PV"
#
# Value is a VTable with columns X, Y, Value, one row of data

from org.csstudio.display.builder.runtime.script import PVUtil

try:
    x = PVUtil.getTableCell(pvs[0], 0, 0)
    xt = "left" if x < 50 else "right"

    y = PVUtil.getTableCell(pvs[0], 0, 1)
    yt = "top" if y > 40 else "bottom"

    v = PVUtil.getTableCell(pvs[0], 0, 2)
    text = "X: %.1f (%s) Y: %.1f (%s) Value: %.1f" % (x, xt, y, yt, v)
except:
    text = ""

widget.setPropertyValue("text", text)
