/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
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

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Paint;
/** View for an Alarm Area. Displays alarm status of all areas on a specified level.
 *  @author Evan Smith
 */
public class AlarmAreaView extends GridPane implements AlarmClientListener
{

	@SuppressWarnings("unused")
	private final AlarmClient model;
	private final AreaFilter areaFilter;

	private final int level = 2;
	private final int col_num = 2;

	private final ConcurrentHashMap</* Item name */String, /* View Item */Label> itemViewMap = new ConcurrentHashMap<>();

    private final Set<String> items_to_add = new LinkedHashSet<>();
    private final Set<String> items_to_remove = new LinkedHashSet<>();
    private final Set<String> items_to_update = new LinkedHashSet<>();

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
		update_throttle.trigger();
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
		update_throttle.trigger();
	}

	// From AlarmClientListener
	@Override
	public void itemUpdated(AlarmTreeItem<?> item)
	{
		final String item_name = areaFilter.filter(item);
		if (null == item_name)
			return;

		synchronized (items_to_update)
        {
            items_to_update.add(item_name);
        }
        update_throttle.trigger();
	}

	// Called  by update_throttle when it triggers.
	private void updateItems()
	{
		final String[] add_array;
		final String[] remove_array;
		final String[] update_array;
		synchronized (items_to_add)
        {
            add_array = items_to_add.toArray(new String[items_to_add.size()]);
            items_to_add.clear();
        }
		synchronized (items_to_remove)
        {
            remove_array = items_to_remove.toArray(new String[items_to_remove.size()]);
            items_to_remove.clear();
        }
        synchronized (items_to_update)
        {
            update_array = items_to_update.toArray(new String[items_to_update.size()]);
            items_to_update.clear();
        }
        Platform.runLater(() ->
        {
        	for (final String item_name : add_array)
        		addItem(item_name);
        	for (final String item_name : remove_array)
        		removeItem(item_name);
	        for (final String item_name : update_array)
	        	updateItem(item_name);
        });
	}

	// Add the label to the grid pane and map the label to its name.
	private void addItem(String item_name)
	{
		final Label label = new Label(item_name);
    	itemViewMap.put(item_name, label);
        getChildren().add(label);
        resetGridConstraints();
	}

	private void removeItem(String item_name)
	{
		final Label label = itemViewMap.get(item_name);
    	getChildren().remove(label);
    	itemViewMap.remove(item_name);
    	areaFilter.removeItem(item_name);
    	resetGridConstraints();
	}

	// Update the items severity.
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
