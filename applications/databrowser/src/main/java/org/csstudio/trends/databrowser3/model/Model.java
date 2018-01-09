/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.csstudio.trends.databrowser3.Messages;

import javafx.scene.paint.Color;

/** Data Browser model
 *  <p>
 *  Maintains a list of {@link ModelItem}s
 *
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto changed the model to accept multiple items with
 *                           the same name so that Data Browser can show the
 *                           trend of the same PV in different axes or with
 *                           different waveform indexes.
 *  @author Megan Grodowitz ported from databrowser 2 to databrowser 3
 */
@SuppressWarnings("nls")
public class Model
{
    /** Listeners to model changes */
    final private List<ModelListener> listeners = new CopyOnWriteArrayList<>();

    /** Axes configurations */
    final private List<AxisConfig> axes = new CopyOnWriteArrayList<AxisConfig>();

    /** All the items in this model */
    final private List<ModelItem> items = new CopyOnWriteArrayList<ModelItem>();

    public String resolveMacros(String name)
    {
        // TODO Move MacroHandler to shared module
        return null;
    }

    /** @param listener New listener to notify */
    public void addListener(final ModelListener listener)
    {
        listeners.add(Objects.requireNonNull(listener));
    }

    /** @param listener Listener to remove */
    public void removeListener(final ModelListener listener)
    {
        listeners.remove(Objects.requireNonNull(listener));
    }

    /** @return Read-only, thread safe {@link AxisConfig}s */
    public Iterable<AxisConfig> getAxes()
    {
        return axes;
    }

    /** Get number of axes
     *
     *  <p>Thread-safe access to multiple axes should use <code>getAxes()</code>
     *
     *  @return Number of axes
     */
    public int getAxisCount()
    {
        return axes.size();
    }

    /** Get specific axis. If the axis for the specified index does not exist, method returns null.
     *
     *  <p>Thread-safe access to multiple axes should use <code>getAxes()</code>
     *
     *  @param index Axis index
     *  @return {@link AxisConfig} or null if the config for the given index does not exist
     */
    public AxisConfig getAxis(final int index)
    {
        try
        {
            return axes.get(index);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return null;
        }
    }

    /** Locate index of value axis
     *  @param axis Value axis configuration
     *  @return Index of axis (0, ...) or -1 if not in Model
     */
    public int getAxisIndex(final AxisConfig axis)
    {
        return axes.indexOf(Objects.requireNonNull(axis));
    }

    /** Add value axis with default settings
     *  @return Newly added axis configuration
     */
    public AxisConfig addAxis()
    {
        final String name = MessageFormat.format(Messages.Plot_ValueAxisNameFMT, axes.size() + 1);
        final AxisConfig axis = new AxisConfig(name);
        axis.setColor(Color.BLACK);
        addAxis(axis);
        return axis;
    }

    /** @param axis New axis to add */
    public void addAxis(final AxisConfig axis)
    {
        axes.add(Objects.requireNonNull(axis));
        axis.setModel(this);
        fireAxisChangedEvent(Optional.empty());
    }

    /** Add axis at given index.
     *  Adding at '1' means the new axis will be at index '1',
     *  and what used to be at '1' will be at '2' and so on.
     *  @param index Index where axis will be placed.
     *  @param axis New axis to add
     */
    public void addAxis(final int index, final AxisConfig axis)
    {
        axes.add(index, Objects.requireNonNull(axis));
        axis.setModel(this);
        fireAxisChangedEvent(Optional.empty());
    }

    /** @param axis Axis to remove
     *  @throws Error when axis not in model, or axis in use by model item
     */
    public void removeAxis(final AxisConfig axis)
    {
        if (! axes.contains(Objects.requireNonNull(axis)))
            throw new Error("Unknown AxisConfig");
        for (ModelItem item : items)
            if (item.getAxis() == axis)
                throw new Error("Cannot removed AxisConfig while in use");
        axis.setModel(null);
        axes.remove(axis);
        fireAxisChangedEvent(Optional.empty());
    }

    /** Notify listeners of changed axis configuration
     *  @param axis Axis that changed, empty to add/remove
     */
    public void fireAxisChangedEvent(final Optional<AxisConfig> axis)
    {
        for (ModelListener listener : listeners)
            listener.changedAxis(axis);
    }

    /** Notify listeners of changed item visibility
     *  @param item Item that changed
     */
    void fireItemVisibilityChanged(final ModelItem item)
    {
        for (ModelListener listener : listeners)
            listener.changedItemVisibility(item);
    }

    /** Notify listeners of changed item configuration
     *  @param item Item that changed
     */
    void fireItemLookChanged(final ModelItem item)
    {
        for (ModelListener listener : listeners)
            listener.changedItemLook(item);
    }

//    /** Notify listeners of changed item configuration
//     *  @param item Item that changed
//     */
//    void fireItemDataConfigChanged(final PVItem item)
//    {
//        for (ModelListener listener : listeners)
//            listener.changedItemDataConfig(item);
//    }
//
//    void fireItemRefreshRequested(final PVItem item)
//    {
//        for (ModelListener listener : listeners)
//            listener.itemRefreshRequested(item);
//    }

    public void fireSelectedSamplesChanged()
    {
        for (ModelListener listener : listeners)
            listener.selectedSamplesChanged();
    }
}
