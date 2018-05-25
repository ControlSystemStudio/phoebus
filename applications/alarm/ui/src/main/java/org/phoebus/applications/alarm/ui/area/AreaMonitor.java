package org.phoebus.applications.alarm.ui.area;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

public class AreaMonitor implements AlarmClientListener
{
	private final AlarmClient model;

	public AreaMonitor(final String server, final String config)
	{
		this.model = new AlarmClient(AlarmSystem.server, AlarmSystem.config_name);
	}

	private void messageFilter(AlarmTreeItem<?> item)
	{

	}

	@Override
	public void itemAdded(AlarmTreeItem<?> item)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void itemRemoved(AlarmTreeItem<?> item)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void itemUpdated(AlarmTreeItem<?> item)
	{
		// TODO Auto-generated method stub

	}
}
