/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.area;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javafx.scene.text.TextAlignment;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;

/** View for an Alarm Area.
 *  Displays alarm status of all areas on a specified level.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmAreaView extends StackPane implements AlarmClientListener
{
    private final ContextMenu menu = new ContextMenu();
    private final GridPane grid = new GridPane();
    private final AreaFilter areaFilter;

    /** Map item name to label in UI that represents the item */
    private final ConcurrentHashMap<String, Label> itemViewMap = new ConcurrentHashMap<>();

    /** As items are added or removed, this is set to the latest list of items.
     *
     *  <p>To keep items sorted by name, it's easier to simply re-create
     *  all item representations (Labels) whenever an item is added or removed.
     *
     *  <p>For a huge number of always changing items that would be inefficient,
     *  but the number of items is small (~10), and the throttle means
     *  we likely only re-create the list a few times when handling the initial flurry
     *  of item additions.
     */
    private final AtomicReference<List<String>> item_changes = new AtomicReference<>();

    /** Set of items that have updated, with iteration order based on when they updated */
    private final Set<String> items_to_update = new LinkedHashSet<>();

    /** Throttle [5Hz] used for updates of existing items */
    private final UpdateThrottle update_throttle = new UpdateThrottle(200, TimeUnit.MILLISECONDS, this::updateItems);

    // For label formatting.
    private final CornerRadii radii = new CornerRadii(10);
    private final BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT, 10, 0, null);
    private final Font font = new Font(AlarmSystem.alarm_area_font_size);
    private final Border border = new Border(new BorderStroke(Color.BLACK, style, radii, new BorderWidths(2)));

    public AlarmAreaView(final AlarmClient model)
    {
        if (model.isRunning())
            throw new IllegalStateException();

        grid.setHgap(AlarmSystem.alarm_area_gap);
        grid.setVgap(AlarmSystem.alarm_area_gap);
        grid.setPadding(new Insets(AlarmSystem.alarm_area_gap));

        getChildren().setAll(grid);

        areaFilter = new AreaFilter(AlarmSystem.alarm_area_level);
        model.addListener(this);

        createContextMenu();
    }

    // AlarmClientModelListener
    @Override
    public void serverStateChanged(final boolean alive)
    {
        Platform.runLater(() ->
        {
            if (alive)
                getChildren().setAll(grid);
            else
                getChildren().setAll(AlarmUI.createNoServerLabel());
        });
    }

    // AlarmClientModelListener
    @Override
    public void serverModeChanged(final boolean maintenance_mode)
    {
        // NOP
    }

    // AlarmClientModelListener
    @Override
    public void serverDisableNotifyChanged(final boolean disable_notify)
    {
        // NOP
    }

    // From AlarmClientListener
    @Override
    public void itemAdded(final AlarmTreeItem<?> item)
    {
        final String item_name = areaFilter.filter(item);
        if (null == item_name)
            return;
        item_changes.set(areaFilter.getItems());
        update_throttle.trigger();
    }

    // From AlarmClientListener
    @Override
    public void itemRemoved(final AlarmTreeItem<?> item)
    {
        final String item_name = areaFilter.filter(item);
        if (null == item_name)
            return;
        areaFilter.removeItem(item_name);
        item_changes.set(areaFilter.getItems());
        update_throttle.trigger();
    }

    // From AlarmClientListener
    @Override
    public void itemUpdated(final AlarmTreeItem<?> item)
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
        final List<String> changed_items = item_changes.getAndSet(null);

        final String[] update_array;
        synchronized (items_to_update)
        {
            update_array = items_to_update.toArray(new String[items_to_update.size()]);
            items_to_update.clear();
        }
        Platform.runLater(() ->
        {
            if (changed_items != null)
                recreateItems(changed_items);
            for (final String item_name : update_array)
                updateItem(item_name);
        });
    }

    /** @param items Items for which to re-create UI */
    private void recreateItems(final List<String> items)
    {
        // Remove all labels from UI and forget their mapping
        itemViewMap.clear();
        grid.getChildren().clear();

        // (Re-)create Label for each item
        int index = 0;
        for (String item_name : items)
        {
            final Label view_item = newAreaLabel(item_name);
            itemViewMap.put(item_name, view_item);
            updateItem(item_name);
            final int column = index % AlarmSystem.alarm_area_column_count;
            final int row = index / AlarmSystem.alarm_area_column_count;
            grid.add(view_item, column, row);
            ++index;
        }
    }

    private Label newAreaLabel(final String item_name)
    {
        final Label label = new Label(item_name);
        label.setBorder(border);
        label.setAlignment(Pos.CENTER);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setFont(font);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        GridPane.setHgrow(label, Priority.ALWAYS);
        GridPane.setVgrow(label, Priority.ALWAYS);
        return label;
    }

    // Update the items severity.
    private void updateItem(final String item_name)
    {
        final Label view_item = itemViewMap.get(item_name);
        if (view_item == null)
        {
            logger.log(Level.WARNING, "Cannot update unknown alarm area item " + item_name);
            return;
        }
        final SeverityLevel severity = areaFilter.getSeverity(item_name);
        final Color color = AlarmUI.getColor(severity);
        view_item.setBackground(new Background(new BackgroundFill(color, radii, Insets.EMPTY)));
        if (color.getBrightness() >= 0.5)
            view_item.setTextFill(Color.BLACK);
        else
            view_item.setTextFill(Color.WHITE);
    }

    private void createContextMenu()
    {
        final ObservableList<MenuItem> menu_items = menu.getItems();

        menu_items.add(new OpenTreeViewAction());
        this.setOnContextMenuRequested(event ->
            menu.show(this.getScene().getWindow(), event.getScreenX(), event.getScreenY())
        );
    }

    public ContextMenu getMenu()
    {
        return menu;
    }
}
