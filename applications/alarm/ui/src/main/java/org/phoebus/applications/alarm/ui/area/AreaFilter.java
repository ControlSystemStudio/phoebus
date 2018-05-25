package org.phoebus.applications.alarm.ui.area;

import org.phoebus.applications.alarm.model.AlarmTreePath;

public class AreaFilter
{
	private int level = 2;


	public AreaFilter(int level)
	{
		// Set level to filter.
		this.level = level;
	}

	public boolean filter(final String path)
	{
		return (AlarmTreePath.splitPath(path).length == level);
	}
}
