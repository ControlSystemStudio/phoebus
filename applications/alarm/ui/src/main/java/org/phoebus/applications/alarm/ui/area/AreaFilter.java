package org.phoebus.applications.alarm.ui.area;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;

public class AreaFilter
{
	private int level = 2;

	public AreaFilter(int level)
	{
		// Set level for levelCheck.
		this.level = level;
	}

	// Check the level of the message by examining the number of levels in its path.
	private boolean levelCheck(final String path)
	{
		return (AlarmTreePath.splitPath(path).length == level);
	}

	// Filter out messages not pertaining to the set level.
	public boolean filter(AlarmTreeItem<?> message)
	{
		if (! levelCheck(message.getPathName()))
			return false;
		return true;
	}
}
