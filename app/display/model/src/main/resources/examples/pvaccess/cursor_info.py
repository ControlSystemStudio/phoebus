# Script that receives a n image widget's "Cursor Info PV"
#
# Value is a VTable with columns X, Y, Value, one row of data

from org.csstudio.display.builder.runtime.script import ValueUtil

table = pvs[0].read()
try:
    x = ValueUtil.getTableCell(table, 0, 0)
    y = ValueUtil.getTableCell(table, 0, 1)
    v = ValueUtil.getTableCell(table, 0, 2)
    text = "X: %.1f Y: %.1f Value: %.1f" % (x, y, v)
except:
    text = ""

widget.setPropertyValue("text", text)
