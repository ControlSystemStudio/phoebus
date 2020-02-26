/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.ui;

import java.lang.ref.WeakReference;

import org.epics.vtype.VType;
import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.SavedValue;
import org.phoebus.applications.pvtable.model.TimestampHelper;
import org.phoebus.applications.pvtable.model.VTypeFormatter;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WeakChangeListener;
import javafx.util.Callback;

/** Proxy for a PVTableItem
 *
 *  <p>TableView works best with property-based data.
 *  Updates to the properties result in table updates,
 *  but such updates must only happen on the UI thread
 *  and be throttled, while the actual PVTableItem
 *  can update at any time on various threads.
 *
 *  <p>This proxy is updated from the underlying PVTableItem
 *  when a UI update is desired.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class TableItemProxy
{
    /** 'Magic' table item added to the end of the actual model to allow adding
     *  entries. Setting the name of this item is handled as adding a new item
     *  for that name.
     */
    final public static TableItemProxy NEW_ITEM = new TableItemProxy();

    /** Callback to obtain list of potentially changing properties.
     *  Add to list wrapped in SortedList to trigger re-sort
     *  when a property changes.
     *  See https://rterp.wordpress.com/2015/05/08/automatically-sort-a-javafx-tableview
     */
    public static final Callback<TableItemProxy, Observable[]> CHANGING_PROPERTIES = proxy -> new Observable[]
    {
        proxy.time,
        proxy.value,
        proxy.alarm,
        proxy.saved,
        proxy.time_saved
    };

    // UI can be held in memory for a little longer after the application
    // has been closed.
    // Use weak reference to application data (PV item)
    // to have that GCed as soon as possible.
    // Eventually, UI will also be GCed.
    final private WeakReference<PVTableItem> item;
    final BooleanProperty selected = new SimpleBooleanProperty();
    final StringProperty name = new SimpleStringProperty("");
    final StringProperty time = new SimpleStringProperty("");
    final StringProperty value = new SimpleStringProperty("");
    final StringProperty desc_value = new SimpleStringProperty("");
    final StringProperty alarm = new SimpleStringProperty("");
    final StringProperty saved = new SimpleStringProperty("");
    final StringProperty time_saved = new SimpleStringProperty("");
    final BooleanProperty use_completion = new SimpleBooleanProperty();

    public TableItemProxy()
    {
        item = null;
    }

    public PVTableItem getItem()
    {
        return item.get();
    }

    public TableItemProxy(final PVTableItem item)
    {
        this.item = new WeakReference<>(item);
        update(item);

        selected.addListener(new WeakChangeListener<>((prop, old, current) ->  item.setSelected(current)));
        use_completion.addListener(new WeakChangeListener<>((prop, old, current) ->  item.setUseCompletion(current)));
    }

    public void update(final PVTableItem item)
    {
        selected.set(item.isSelected());
        name.set(item.getName());

        final VType item_value = item.getValue();
        if (item_value == null)
        {
            time.set("");
            value.set("");
            alarm.set("");
        }
        else
        {
            time.set(TimestampHelper.format(org.phoebus.core.vtypes.VTypeHelper.getTimestamp(item_value)));
            value.set(VTypeFormatter.toString(item_value));
            alarm.set(VTypeFormatter.formatAlarm(item_value));
        }

        desc_value.set(item.getDescription());

        final SavedValue saved_value = item.getSavedValue().orElse(null);
        if (saved_value == null)
        {
            saved.set("");
            time_saved.set("");
        }
        else
        {
            saved.set(saved_value.toString());
            time_saved.set(item.getTime_saved());
        }

        use_completion.set(item.isUsingCompletion());
    }
}
