/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import static org.phoebus.applications.pvtable.PVTableApplication.logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.phoebus.applications.pvtable.Settings;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.jobs.JobManager;

import javafx.application.Platform;

/** A PV table model, i.e. list of {@link PVTableItem}s
 *
 *  <p>Updates are throttled: Changed items are accumulated, and depending on how
 *  many changed, just those items are notified, or the whole table is marked for
 *  update.
 *
 *  @author Kay Kasemir, A. PHILIPPE L. PHILIPPE GANIL/FRANCE
 */
public class PVTableModel implements PVTableItemListener
{
    /** Period for update checks
     *
     *  @see #performUpdates()
     */
    private static final long UPDATE_PERIOD_MS = 200;

    /** The list of items in this table. */
    private List<PVTableItem> items = new ArrayList<PVTableItem>();

    final private List<PVTableModelListener> listeners = new ArrayList<PVTableModelListener>();

    final private Timer update_timer = new Timer("PVTableUpdate", true); //$NON-NLS-1$

    /** @see #performUpdates() */
    private Set<PVTableItem> changed_items = new HashSet<PVTableItem>();

    private Configuration config = null;

    /** The number of time the measure was done */
    private int nbMeasure = 1;

    /** Timeout in seconds used for restoring PVs with completion */
    private long completion_timeout_seconds = 60;

    /** Initialize */
    public PVTableModel()
    {
        update_timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                performUpdates();
            }
        }, UPDATE_PERIOD_MS, UPDATE_PERIOD_MS);
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

    /** @return Returns number of items (rows) in model. */
    public int getItemCount()
    {
        return items.size();
    }

    /** @return Returns item (row). */
    public PVTableItem getItem(final int row)
    {
        if (row >= 0 && row < items.size())
            return items.get(row);
        return null;
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

    /**
     * @return config null if no config
     */
    public Configuration getConfig() {
        if (config == null) {
            return null;
        }
        return config;
    }

    /**
     * Set nbMeasure
     *
     * @param nbMeasure
     */
    public void setNbMeasure(int nbMeasure) {
        this.nbMeasure = nbMeasure;
    }

    /**
     * get nbMeasure
     *
     * @return
     */
    public int getNbMeasure() {
        return this.nbMeasure;
    }

    /** Add table item
     *  @param pv_name PV Name
     *  @return Added item
     */
    public PVTableItem addItem(final String pv_name)
    {
        return addItem(new PVTableItem(pv_name, null, false, null, Settings.tolerance, null, this));
    }

    /** Add table item
     *
     *  @param pv_name
     *  @param tolerance
     *  @param saved
     *  @param time
     *  @param conf
     *  @return added item
     */
    public PVTableItem addItem(final String pv_name, final double tolerance, final SavedValue saved, final String time,
                               final boolean conf, final Measure measure)
    {
        return addItem(new PVTableItem(pv_name, time, conf, measure, tolerance, saved, this));
    }

    /** Add table item Check if the item is a conf or not
     *
     *  @param item Item to add
     *  @return Added item
     */
    public PVTableItem addItem(final PVTableItem item)
    {
        // A conf already exist
        if (item.isMeasureHeader() && !items.contains(item))
            this.nbMeasure++;
        items.add(item);
        // Add a conf header
        this.isConfHeaderToAdd(item);
        // Add an item witch is not a conf header
        // and there are no conf anymore
        if (config != null)
        {
            // If the item is not the first one in the table, that a conf
            // already exist and that the item
            // above it belongs to the conf, add it to the conf.
            if (items.indexOf(item) > 0 && items.get(items.indexOf(item) - 1).isConf() == true
                    && !item.isMeasureHeader())
            {
                config.addItem(item);
                item.setConf(true);
            }
        }
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
        return addItemAbove(item, new PVTableItem(pv_name, Settings.tolerance, null, this));
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
        // Add an item witch is not a conf header
        // and there are no conf anymore
        if (config != null)
        {
            // If the item is not the first one in the table, that a conf
            // already exist and that the item
            // above it belongs to the conf, add it to the conf.
            if (items.indexOf(newItem) > 0 && items.get(items.indexOf(newItem) - 1).isConf() == true
                    && !newItem.isMeasureHeader())
            {
                config.addItem(newItem);
                newItem.setConf(true);
            }
        }
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
        if (item.isConfHeader()) {
            // for each config item the item confItem is remove from the conf.
            for (PVTableItem confItem : config.getItems())
                confItem.setConf(false);
            config = null;
        }
        if (item.isConf())
            config.removeItem(item);
        items.remove(item);
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

    /**
     * Check if the item is a confHeader, if a configuration already exist and
     * create it if not.
     *
     * @param item
     */
    public void isConfHeaderToAdd(PVTableItem item)
    {

        if (item.isConfHeader()) {
            // If doesn't exist any conf
            if (config == null) {
                // Create the conf
                config = new Configuration(item);
                // If rows below, add them to conf
                int indexHeader = items.indexOf(item);
                for (int i = indexHeader; i < items.size(); i++) {
                    if (items.get(i).isMeasureHeader() == false && items.get(i).isMeasure() == false) {
                        items.get(i).setConf(true);
                        config.addItem(items.get(i));
                    }
                }
            } else {
                for (PVTableItem i : items) {
                    if (i.isConfHeader()) {
                        logger.log(Level.WARNING, "Cannot add more than one config header");
                        item.updateName(item.getName());
                        return;
                    }
                }
                config = new Configuration(item);
                // If rows below, add them to conf
                int indexHeader = items.indexOf(item);
                for (int i = indexHeader; items.get(items.indexOf(item)).isMeasure() == false
                        && i < items.size(); i++) {
                    items.get(i).setConf(true);
                    config.addItem(items.get(i));
                }
            }
        }
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
        isConfHeaderToAdd(item);
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

    public void saveConf()
    {
        save(config.getItems());
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
