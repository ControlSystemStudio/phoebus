package org.phoebus.applications.alarm.ui.area;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Paint;

public class AlarmAreaView extends GridPane implements AlarmClientListener
{

	@SuppressWarnings("unused")
	private final AlarmClient model;
	private final AreaFilter areaFilter;

	private final int level = 2;
	private final int col_num = 2;

	private final ConcurrentHashMap<String, /* View Item */Label> itemViewMap = new ConcurrentHashMap<>();

    private final Set<String> items_to_add = new LinkedHashSet<>();
    private final Set<String> items_to_remove = new LinkedHashSet<>();
    private final Set<String> items_to_update = new LinkedHashSet<>();

    /** Throttle [5Hz] used for adding items */
    private final UpdateThrottle add_throttle = new UpdateThrottle(200, TimeUnit.MILLISECONDS, this::addItems);
    /** Throttle [5Hz] used for removal of existing items */
    private final UpdateThrottle remove_throttle = new UpdateThrottle(200, TimeUnit.MILLISECONDS, this::removeItems);
    /** Throttle [5Hz] used for updates of existing items */
    private final UpdateThrottle update_throttle = new UpdateThrottle(200, TimeUnit.MILLISECONDS, this::updateItems);

	public AlarmAreaView(AlarmClient model)
	{
		if (model.isRunning())
            throw new IllegalStateException();

        this.model = model;
        areaFilter = new AreaFilter(level);
        model.addListener(this);
	}

	// From AlarmClientListener
	@Override
	public void itemAdded(AlarmTreeItem<?> item)
	{
		final String item_name = areaFilter.filter(item);
		if (null == item_name)
			return;
		synchronized(items_to_add)
		{
			items_to_add.add(item_name);
		}
		add_throttle.trigger();
	}

	// Called by add_throttle when it triggers.
	private void addItems()
	{
		final String[] items;
        synchronized (items_to_add)
        {
            items = items_to_add.toArray(new String[items_to_add.size()]);
            items_to_add.clear();
        }

        for (final String item : items)
           addItem(item);
	}

	// Add the label to the grid pane and map the label to its name.
	private void addItem(String item_name)
	{
		final Label label = new Label(item_name);
    	itemViewMap.put(item_name, label);
        getChildren().add(label);
        resetGridConstraints();
	}

	// From AlarmClientListener
	@Override
	public void itemRemoved(AlarmTreeItem<?> item)
	{
		final String item_name = areaFilter.filter(item);
		if (null == item_name)
			return;
		synchronized(items_to_remove)
		{
			items_to_remove.add(item_name);
		}
		remove_throttle.trigger();
	}

	public void removeItems()
	{
		final String[] items;
        synchronized (items_to_remove)
        {
            items = items_to_remove.toArray(new String[items_to_remove.size()]);
            items_to_remove.clear();
        }
        for (final String item : items)
           removeItem(item);
	}

	private void removeItem(String item_name)
	{
		final Label label = itemViewMap.get(item_name);
    	getChildren().remove(label);
    	itemViewMap.remove(item_name);
    	resetGridConstraints();
	}

	// From AlarmClientListener
	@Override
	public void itemUpdated(AlarmTreeItem<?> item)
	{
		final String item_name = areaFilter.filter(item);
		if (null == item_name)
			return;
		//System.out.println(item.getName() + " updated.");
		synchronized (items_to_update)
        {
            items_to_update.add(item_name);
        }
        update_throttle.trigger();
	}

	private void updateItems()
	{
		final String[] items;
        synchronized (items_to_update)
        {
            items = items_to_update.toArray(new String[items_to_update.size()]);
            items_to_update.clear();
        }

        for (final String item_name : items)
            updateItem(item_name);
	}

	private void updateItem(String item_name)
	{
		final SeverityLevel severity = areaFilter.getSeverity(item_name);
		final Paint color = AlarmUI.getColor(severity);
		final Label label = itemViewMap.get(item_name);
		label.setTextFill(color);
	}

	private void resetGridConstraints()
	{
		int index = 0;
		for (final Node child : getChildren())
		{
			final int columnIndex = index%col_num;
			final int rowIndex = index/col_num;

			setConstraints(child, columnIndex, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(10, 10, 10, 10));
			index++;
		}
	}
}
