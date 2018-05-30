package org.phoebus.applications.alarm.ui.area;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.SeverityLevel;

public class AreaFilter
{
	private final int level;

	private final ConcurrentHashMap<String, SeverityLevel> itemSeverity = new ConcurrentHashMap<>();

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
	public String filter(AlarmTreeItem<?> message)
	{
		if (! levelCheck(message.getPathName()))
			return null;
		final String name = message.getName();
		if (null == itemSeverity.get(name))
			itemSeverity.put(name, message.getState().getSeverity());
		return message.getName();
	}

	public List<String> getItems()
	{
		return Collections.list(itemSeverity.keys());
	}

	public SeverityLevel getSeverity(String item_name)
	{
		return itemSeverity.get(item_name);
	}

	public void removeItem(String item_name)
	{
		itemSeverity.remove(item_name);
	}
}
