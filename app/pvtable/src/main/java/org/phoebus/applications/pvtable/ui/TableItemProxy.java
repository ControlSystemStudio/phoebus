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
import org.phoebus.applications.pvtable.model.VTypeHelper;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WeakChangeListener;

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
class TableItemProxy
{
    /** 'Magic' table item added to the end of the actual model to allow adding
     *  entries. Setting the name of this item is handled as adding a new item
     *  for that name.
     */
    final public static TableItemProxy NEW_ITEM = new TableItemProxy();

    // UI can be held in memory for a little longer after the application
    // has been closed.
    // Use weak reference to application data (PV item)
    // to have that GCed as soon as possible.
    // Eventually, UI will also be GCed.
    final private WeakReference<PVTableItem> item;
    final BooleanProperty selected = new SimpleBooleanProperty();
    final StringProperty name = new SimpleStringProperty();
    final StringProperty time = new SimpleStringProperty();
    final StringProperty value = new SimpleStringProperty();
    final StringProperty desc_value = new SimpleStringProperty();
    final StringProperty alarm = new SimpleStringProperty();
    final StringProperty saved = new SimpleStringProperty();
    final StringProperty time_saved = new SimpleStringProperty();
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
            time.set(null);
            value.set(null);
            alarm.set(null);
        }
        else
        {
            time.set(TimestampHelper.format(VTypeHelper.getTimestamp(item_value)));
            value.set(VTypeHelper.toString(item_value));
            alarm.set(VTypeHelper.formatAlarm(item_value));
        }

        desc_value.set(item.getDescription());

        final SavedValue saved_value = item.getSavedValue().orElse(null);
        if (saved_value == null)
        {
            saved.set(null);
            time_saved.set(null);
        }
        else
        {
            saved.set(saved_value.toString());
            time_saved.set(item.getTime_saved());
        }

        use_completion.set(item.isUsingCompletion());
    }
}
