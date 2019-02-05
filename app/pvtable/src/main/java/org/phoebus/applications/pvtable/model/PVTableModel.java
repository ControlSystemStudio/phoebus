/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.phoebus.applications.pvtable.Settings;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.application.Platform;

/** A PV table model, i.e. list of {@link PVTableItem}s
 *
 *  <p>Updates are throttled: Changed items are accumulated, and depending on how
 *  many changed, just those items are notified, or the whole table is marked for
 *  update.
 *
 *  @author Kay Kasemir, A. PHILIPPE L. PHILIPPE GANIL/FRANCE
 */
@SuppressWarnings("nls")
public class PVTableModel implements PVTableItemListener
{
    /** Period for update checks
     *
     *  @see #performUpdates()
     */
    private static final long UPDATE_PERIOD_MS = 200;

    private volatile BooleanSupplier suppress_updates = () -> false;

    private boolean enableSaveRestore = true;

    /** The list of items in this table. */
    private List<PVTableItem> items = new ArrayList<>();

    final private List<PVTableModelListener> listeners = new ArrayList<>();

    private Timer update_timer;

    /** @see #performUpdates() */
    private Set<PVTableItem> changed_items = new HashSet<>();

    /** Timeout in seconds used for restoring PVs with completion */
    private long completion_timeout_seconds = 60;

    /** Initialize */
    public PVTableModel()
    {
        this(true);
    }

    /** Initialize
     *  @param with_timer With timer to send events?
     */
    public PVTableModel(final boolean with_timer)
    {
        if (with_timer)
        {
            update_timer = new Timer("PVTableUpdate", true);
            update_timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    performUpdates();
                }
            }, UPDATE_PERIOD_MS, UPDATE_PERIOD_MS);
        }
    }

    /** @param suppress_updates Function that can suppress updates by returning <code>true</code> */
    public void setUpdateSuppressor(final BooleanSupplier suppress_updates)
    {
        this.suppress_updates = suppress_updates;
    }

    /** @param listener Listener to add */
    public void addListener(final PVTableModelListener listener)
    {
        listeners.add(listener);
    }

    /** @param listener Listener to remove */
    public void removeListener(final PVTableModelListener listener)
    {
        listeners.remove(listener);
    }

    /** @return All items */
    public List<PVTableItem> getItems()
    {
        return items;
    }

    /** @return Timeout in seconds used for restoring PVs with completion */
    public long getCompletionTimeout()
    {
        return completion_timeout_seconds;
    }

    /** @param seconds Timeout in seconds used for restoring PVs with completion */
    public void setCompletionTimeout(final long seconds)
    {
        completion_timeout_seconds = seconds;
    }

    /** Add table item
     *  @param pv_name PV Name
     *  @return Added item
     */
    public PVTableItem addItem(final String pv_name)
    {
        return addItem(new PVTableItem(pv_name, Settings.tolerance, this));
    }

    /** Add table item
     *
     *  @param pv_name PV Name
     *  @param tolerance Tolerance
     *  @param saved {@link SavedValue}
     *  @return Added item
     */
    public PVTableItem addItem(final String pv_name, final double tolerance, final SavedValue saved, final String time_saved)
    {
        return addItem(new PVTableItem(pv_name, Settings.tolerance, saved, time_saved, this));
    }

    /** Add table item
     *
     *  @param item Item to add
     *  @return Added item
     */
    public PVTableItem addItem(final PVTableItem item)
    {
        items.add(item);
        for (PVTableModelListener listener : listeners)
            listener.modelChanged();
        return item;
    }

    /** Add a new item above an already existing item
     *
     *  @param item Existing item. <code>null</code> to add at bottom.
     *  @param pv_name PV name (or comment) of new item
     *  @return Added item
     */
    public PVTableItem addItemAbove(final PVTableItem item, final String pv_name)
    {
        return addItemAbove(item, new PVTableItem(pv_name, Settings.tolerance, this));
    }

    /** Add a new item above the selected row. And check if this new item is
     *  added to a configuration.
     *
     *  @param item
     *  @param newItem
     *  @return newItem
     */
    public PVTableItem addItemAbove(final PVTableItem item, final PVTableItem newItem)
    {
        if (item == null)
            return addItem(newItem);

        final int index = Math.max(0, items.indexOf(item));
        items.add(index, newItem);
        for (PVTableModelListener listener : listeners)
            listener.modelChanged();
        return newItem;
    }

    /** Remove table item (also disposes it)
     *
     *  @param item Item to remove from model
     */
    public void removeItem(final PVTableItem item)
    {
        item.dispose();
        items.remove(item);
        for (PVTableModelListener listener : listeners)
            listener.modelChanged();
    }

    /** Transfer items from other model to this one
     *
     *  <p>Updates from items will be lost during the transfer,
     *  but a final 'modelChanged' event is sent to listeners
     *  of this model.
     *
     *  @param other_model Model from which all items will be removed, then added into this model
     */
    public void transferItems(final PVTableModel other_model)
    {
        for (PVTableItem item : other_model.items)
        {
            item.listener = this;
            items.add(item);
        }
        setSaveRestore(other_model.isSaveRestoreEnabled());
        other_model.items.clear();
        for (PVTableModelListener listener : listeners)
            listener.modelChanged();
    }

    /** Invoked by timer to perform accumulated updates.
     *
     *  <p>If only one item changed, update that item. If multiple items changed,
     *  refresh the whole table.
     */
    private void performUpdates()
    {
        if (suppress_updates.getAsBoolean())
        {
            // System.out.println("Suppressing updates");
            return;
        }
        final List<PVTableItem> to_update = new ArrayList<>();
        synchronized (changed_items)
        {
            // Lock changed_items as briefly as possible to check what changed
            final int changed = changed_items.size();
            // Anything?
            if (changed <= 0)
                return;
            // Limited number, update those items?
            if (changed < Settings.update_item_threshold)
                to_update.addAll(changed_items);
            // else: Many items, update whole table
            changed_items.clear();
        }
        Platform.runLater(() ->
        {
            if (to_update.isEmpty())
            {
                // Too many items changed, update the whole table
                for (PVTableModelListener listener : listeners)
                    listener.tableItemsChanged();
            }
            else
            {
                // Update exactly the changed items
                for (PVTableItem item : to_update)
                    for (PVTableModelListener listener : listeners)
                        listener.tableItemChanged(item);
            }
        });
    }

    /** In case updates were suppressed while editing,
     *  this call performs the queued changes
     */
    public void performPendingUpdates()
    {
        synchronized (changed_items)
        {
            if (changed_items.isEmpty())
                return;
        }
        performUpdates();
    }

    /** {@inheritDoc} */
    @Override
    public void tableItemSelectionChanged(final PVTableItem item)
    {
        // Model receives this from item. Forward to listeners of model
        for (PVTableModelListener listener : listeners)
            listener.tableItemSelectionChanged(item);
    }

    /** {@inheritDoc} */
    @Override
    public void tableItemChanged(final PVTableItem item)
    {
        synchronized (changed_items)
        {
            changed_items.add(item);
        }
    }

    /** Save snapshot value of all checked items */
    public void save()
    {
        for (PVTableItem item : items)
            if (item.isSelected())
                item.save();

        for (PVTableModelListener listener : listeners)
        {
            listener.tableItemsChanged();
            listener.modelChanged();
        }
    }

    /** Save snapshot value of each item
     *
     *  @param items Items to save
     */
    public void save(final List<PVTableItem> items)
    {
        for (PVTableItem item : items)
            item.save();
        for (PVTableModelListener listener : listeners)
        {
            listener.tableItemsChanged();
            listener.modelChanged();
        }
    }

    public void setSaveRestore(boolean enableSaveRestore)
    {
        this.enableSaveRestore = enableSaveRestore;
    }

    public boolean isSaveRestoreEnabled()
    {
        return enableSaveRestore;
    }

    /** Restore saved values for all checked items */
    public void restore()
    {
        final List<PVTableItem> selected = items.stream()
                                                .filter(PVTableItem::isSelected)
                                                .collect(Collectors.toList());
        restore(selected);
    }

    /** Restore saved values
     *  @param items Items to restore
     */
    public void restore(final List<PVTableItem> items)
    {
        // Perform in background task
        JobManager.schedule("Restore PV Table", monitor ->
        {
            monitor.beginTask("Restore PVs", items.size());
            for (PVTableItem item : items)
            {
                try
                {
                    item.restore(completion_timeout_seconds);
                }
                catch (Exception ex)
                {
                    ExceptionDetailsErrorDialog.openError("Error",
                            "Error restoring value for PV " + item.getName(), ex);
                    return;
                }
                monitor.worked(1);
            }
        });
    }

    /** Must be invoked when 'done' by the creator of the model. */
    public void dispose()
    {
        update_timer.cancel();
        for (PVTableItem item : items)
            item.dispose();
        items.clear();
    }

    /** Inform listeners that model changed */
    public void fireModelChange()
    {
        for (PVTableModelListener listener : listeners)
            listener.modelChanged();
    }
}
