from org.csstudio.display.builder.runtime.script import PVUtil, ScriptUtil

def findConflicts(row, col, value):
	conflicts = []
	if value == " " or value == "_":
		return conflicts
	for currCol in range(9):
		if currCol != col and \
                value == PVUtil.getString(pvs[calcPVIndex(row,currCol)]):
			conflicts.append((row, currCol))
		for currRow in range(9):
			if currRow != row and \
                	value == PVUtil.getString(pvs[calcPVIndex(currRow,col)]):
				conflicts.append((currRow, col))
	for currRow in calcBlockRange(row):
		for currCol in calcBlockRange(col):
			if currRow != row and \
                   currCol != col and \
                   value == PVUtil.getString(pvs[calcPVIndex(currRow,currCol)]):
				conflicts.append((currRow,currCol))
	if len(conflicts) > 0:
		conflicts.append((row,col))
	return conflicts

def clearConflictsDisplay():
    if PVUtil.getDouble(pvs[1]) != 0: return
    for row in range(9):
        for col in range(9):
            cell = getWidget(row, col)
            pv = pvs[calcPVIndex(row,col)]
            if PVUtil.getString(pv) == "#":
                pv.write(" ")
            cell.setPropertyValue("foreground_color", text)
    #logger.warning("finished clearing conflicts")

selected = int(widget.getPropertyValue("name")[4:])
value = PVUtil.getString(pvs[0])
#logger.warning("*Entry for %d" % selected)
if value != "#" and PVUtil.getDouble(pvs[1]) != -1:
    col = selected%10
    row = (selected-col)/10
    if value != " ":
    	clearConflictsDisplay()
    conflicts = findConflicts(row, col, value)
    if value == "_":
    	pvs[0].write(" ")
    elif len(conflicts) > 0:
        pvs[0].write("#")
        #logger.warning("conflicted (%d,%d)" % (row, col))
        for pos in conflicts:
            cell = getWidget(pos[0], pos[1])
            cell.setPropertyValue("foreground_color",
                                  WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR))
    #resume normal behavior after auto-solve
    if selected == 88:
        pvs[1].write(0)
#endif (value != "#", etc.)"""
