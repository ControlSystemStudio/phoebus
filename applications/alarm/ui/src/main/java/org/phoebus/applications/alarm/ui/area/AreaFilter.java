package org.phoebus.applications.alarm.ui.area;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.phoebus.applications.alarm.model.AlarmTreeItem;

public class AreaFilter
{
	// Map associating components to their paths.
	private final ConcurrentHashMap<String, AlarmTreeItem<?>> path2comp = new ConcurrentHashMap<>();
	private int level = 2;
	//private final Set<<AlarmTreeItem<?>> items_to_update = new LinkedHashSet<>();

	public AreaFilter(List<AlarmTreeItem<?>> components, int level)
	{
		this.level = level;
		for (final AlarmTreeItem<?> component : components)
			path2comp.put(component.getPathName(), component);
	}

	private AlarmTreeItem<?> messageFilter(final String path)
	{
		return path2comp.get(path);
	}
}
