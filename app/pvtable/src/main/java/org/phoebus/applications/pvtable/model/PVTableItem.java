/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import static org.phoebus.applications.pvtable.PVTableApplication.logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.applications.pvtable.Settings;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import org.phoebus.core.vtypes.VTypeHelper;

import io.reactivex.disposables.Disposable;

/** One item (row) in the PV table.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableItem
{
    volatile PVTableItemListener listener;

    private volatile boolean selected = true;

    /** Primary PV name */
    private String name = null;

    /** Last known value of the PV */
    private volatile VType value;

    /** Value of the PV's description */
    private volatile String desc_value = "";

    /** Saved (snapshot) value */
    private volatile Optional<SavedValue> saved = Optional.empty();

    /** TimeStamp Saved */
    private volatile String time_saved = "";

    /** Does current value differ from saved value? */
    private volatile boolean has_changed;

    /** Tolerance for comparing current and saved (if they're numeric) */
    private double tolerance;

    private volatile boolean use_completion = true;

    /** Primary PV */
    final private AtomicReference<PV> pv = new AtomicReference<>(null);
    /** Listener to primary PV */
    private volatile Disposable value_flow, permission_flow;

    /** Description PV */
    final private AtomicReference<PV> desc_pv = new AtomicReference<>(null);

    /** Listener to description PV */
    private volatile Disposable desc_flow;

    /** Initialize
     *
     *  @param name
     *  @param tolerance
     *  @param listener
     */
    PVTableItem(final String name, final double tolerance, final PVTableItemListener listener)
    {
        this(name, tolerance, null, "", listener);
    }

    /** Initialize
     *
     *  @param name
     *  @param tolerance
     *  @param saved
     *  @param listener
     */
    PVTableItem(final String name, final double tolerance,
                final SavedValue saved,
                final String time_saved,
                final PVTableItemListener listener)
    {
        this.listener = listener;
        this.tolerance = tolerance;
        this.saved = Optional.ofNullable(saved);
        this.time_saved = time_saved;
        createPVs(name);
        determineIfChanged();
    }

    /** Set PV name and create reader/writer
     *
     *  @param name Primary PV name
     */
    private void createPVs(final String name)
    {
        this.name = name;
        // Ignore empty PVs or comments
        if (name.isEmpty() || isComment())
        {
            updateValue(null);
            return;
        }
        try
        {
            updateValue(VString.of("", Alarm.disconnected(), Time.now()));
            final PV new_pv = PVPool.getPV(name);
            value_flow = new_pv.onValueEvent()
                               .throttleLatest(Settings.max_update_period_ms, TimeUnit.MILLISECONDS)
                               .subscribe(this::updateValue);
            permission_flow = new_pv.onAccessRightsEvent()
                                    .subscribe(writable -> listener.tableItemChanged(PVTableItem.this));
            pv.set(new_pv);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create PV " + name, ex);
            updateValue(VString.of("PV Error",
                                   Alarm.of(AlarmSeverity.UNDEFINED, AlarmStatus.CLIENT, "No PV"),
                                   Time.now()));
        }

        // For CA PVs, check the .DESC field
        // Hardcoded knowledge to avoid non-record PVs.
        if (Settings.show_description &&
            ! (name.startsWith("sim:") ||
               name.startsWith("loc:")))
        {
            // Determine DESC field.
            // If name already includes a field,
            // replace it with DESC field.
            final int sep = name.lastIndexOf('.');
            final String desc_name = sep >= 0
                    ? name.substring(0, sep) + ".DESC"
                    : name + ".DESC";
            try
            {
                final PV new_desc_pv = PVPool.getPV(desc_name);
                desc_flow = new_desc_pv.onValueEvent()
                                       .throttleLatest(Settings.max_update_period_ms, TimeUnit.MILLISECONDS)
                                       .subscribe(value ->
                {
                    if (value instanceof VString)
                        desc_value = ((VString) value).getValue();
                    else
                        desc_value = "";
                    listener.tableItemChanged(PVTableItem.this);
                });
                desc_pv.set(new_desc_pv);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Skipping " + desc_name);
            }
        }
    }

    /** @return <code>true</code> if item is selected to be restored */
    public boolean isSelected()
    {
        return selected && !isComment();
    }

    /** @param selected Should item be selected to be restored? */
    public void setSelected(final boolean selected)
    {
        boolean was_selected = this.selected;
        this.selected = selected && !isComment();
        if (was_selected != this.selected)
            listener.tableItemSelectionChanged(this);
    }

    /** @return Returns the name of the 'main' PV. */
    public String getName()
    {
        return name;
    }

    /** @return Returns the comment. */
    public String getComment()
    {
        // Skip initial "#". Trim in case of another space from "# "
        return name.substring(1).trim();
    }

    /** Update PV name
     *
     *  <p>Also resets saved and current value,
     *  since it no longer applies to the new name.
     *
     *  @param new_name PV Name
     *  @return <code>true</code> if name was indeed changed
     */
    public boolean updateName(final String new_name)
    {
        if (name.equals(new_name))
            return false;
        dispose();
        saved = Optional.empty();
        time_saved = "";
        value = null;
        has_changed = false;
        createPVs(new_name);
        return true;
    }

    /** @param new_value New value of item */
    protected void updateValue(final VType new_value)
    {
        value = new_value;
        determineIfChanged();
        listener.tableItemChanged(this);
    }

    /** @return Value */
    public VType getValue()
    {
        return value;
    }

    /** @return Description */
    public String getDescription()
    {
        return desc_value;
    }

    /** @return Enum options for current value, <code>null</code> if not enumerated */
    public String[] getValueOptions()
    {
        final VType copy = value;
        if (!(copy instanceof VEnum))
            return null;
        final List<String> options = ((VEnum) copy).getDisplay().getChoices();
        return options.toArray(new String[options.size()]);
    }

    /** @return <code>true</code> when PV is writable */
    public boolean isWritable()
    {
        final PV the_pv = pv.get();
        return the_pv != null && the_pv.isReadonly() == false && !isComment();
    }

    /** @return Await completion when restoring value to PV? */
    public boolean isUsingCompletion()
    {
        return use_completion;
    }

    /** @param use_completion Await completion when restoring value to PV? */
    public void setUseCompletion(final boolean use_completion)
    {
        this.use_completion = use_completion;
    }

    /** @param new_value Value to write to the item's PV */
    public void setValue(String new_value)
    {
        new_value = new_value.trim();
        try
        {
            final PV the_pv = pv.get();
            if (the_pv == null)
                throw new Exception("Not connected");

            final VType pv_type = the_pv.read();
            if (pv_type instanceof VNumber)
            {
                if (Settings.show_units)
                {   // Strip units so that only the number gets written
                    final String units = ((VNumber)pv_type).getDisplay().getUnit();
                    if (units.length() > 0  &&  new_value.endsWith(units))
                        new_value = new_value.substring(0, new_value.length() - units.length()).trim();
                }
                the_pv.write(Double.parseDouble(new_value));
            }
            else if (pv_type instanceof VEnum)
            { // Value is displayed as "6 =
              // 1 second"
                // Locate the initial index, ignore following text
                final int end = new_value.indexOf(' ');
                final int index = end > 0
                        ? Integer.valueOf(new_value.substring(0, end))
                        : Integer.valueOf(new_value);
                the_pv.write(index);
            }
            else if (pv_type instanceof VByteArray && Settings.treat_byte_array_as_string)
            {
                // Write string as byte array WITH '\0' TERMINATION!
                final byte[] bytes = new byte[new_value.length() + 1];
                System.arraycopy(new_value.getBytes(), 0, bytes, 0,
                        new_value.length());
                bytes[new_value.length()] = '\0';
                the_pv.write(bytes);
            }
            else if (pv_type instanceof VNumberArray)
            {
                final String[] elements = new_value.split("\\s*,\\s*");
                final int N = elements.length;
                final double[] data = new double[N];
                for (int i = 0; i < N; ++i)
                    data[i] = Double.parseDouble(elements[i]);
                the_pv.write(data);
            }
            else if (pv_type instanceof VEnumArray)
            {
                final String[] elements = new_value.split("\\s*,\\s*");
                final int N = elements.length;
                final int[] data = new int[N];
                for (int i = 0; i < N; ++i)
                    data[i] = (int) Double.parseDouble(elements[i]);
                the_pv.write(data);
            }
            else // Write other types as string
                the_pv.write(new_value);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot set " + getName() + " = " + new_value, ex);
        }
    }

    /** Save current value as saved value, w/ timestamp */
    public void save()
    {
        if (isComment())
            return;

        try
        {
            if (value == null  ||  VTypeHelper.getSeverity(value).compareTo(AlarmSeverity.INVALID) >= 0)
            {   // Nothing to save
                time_saved = "";
                saved = Optional.empty();
            }
            else
            {
                time_saved = TimestampHelper.format(VTypeHelper.getTimestamp(value));
                saved = Optional.of(SavedValue.forCurrentValue(value));
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot save value of " + getName(), ex);
        }
        determineIfChanged();
    }

    /** @return time_saved, the timestamp saved */
    public String getTime_saved()
    {
        return time_saved;
    }

    /** @param time_saved, the current value of timestamp */
    public void setTime_saved(String time_saved)
    {
        this.time_saved = time_saved;
    }

    /** Write saved value back to PV
     *  @param completion_timeout_seconds
     *  @throws Exception on error
     */
    public void restore(final long completion_timeout_seconds) throws Exception
    {
        if (isComment() || !isWritable())
            return;

        final PV the_pv = pv.get();
        final SavedValue the_value = saved.orElse(null);
        if (the_pv == null || the_value == null)
            return;

        the_value.restore(the_pv, isUsingCompletion() ? completion_timeout_seconds : 0);
    }

    /** @return Returns the saved_value */
    public Optional<SavedValue> getSavedValue()
    {
        return saved;
    }

    /** @return Tolerance for comparing saved and current value */
    public double getTolerance()
    {
        return tolerance;
    }

    /** @param tolerance Tolerance for comparing saved and current value */
    public void setTolerance(final double tolerance)
    {
        this.tolerance = tolerance;
        determineIfChanged();
        listener.tableItemChanged(this);
    }

    /** @return <code>true</code> if this item is a comment instead of a PV with
     *          name, value etc.
     */
    public boolean isComment()
    {
        return name.startsWith("#");
    }

    /** @return <code>true</code> if value has changed from saved value */
    public boolean isChanged()
    {
        return has_changed;
    }

    /** Update <code>has_changed</code> based on current and saved value */
    private void determineIfChanged()
    {
        if (isComment())
        {
            has_changed = false;
            return;
        }
        final Optional<SavedValue> saved_value = saved;
        if (!saved_value.isPresent())
        {
            has_changed = false;
            return;
        }
        try
        {
            has_changed = !saved_value.get().isEqualTo(value, tolerance);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Change test failed for " + getName(), ex);
        }
    }

    /** Must be called to release resources when item no longer in use */
    public void dispose()
    {
        PV the_pv = pv.getAndSet(null);
        if (the_pv != null)
        {
            permission_flow.dispose();
            value_flow.dispose();
            PVPool.releasePV(the_pv);
        }

        the_pv = desc_pv.getAndSet(null);
        if (the_pv != null)
        {
            desc_flow.dispose();
            PVPool.releasePV(the_pv);
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(name);
        if (!isWritable())
            buf.append(" (read-only)");

        buf.append(" = ").append(VTypeHelper.toString(value));
        final Optional<SavedValue> saved_value = saved;
        if (saved_value.isPresent())
        {
            if (has_changed)
                buf.append(" ( != ");
            else
                buf.append(" ( == ");
            buf.append(saved_value.get().toString()).append(" +- ")
                    .append(tolerance).append(")");
        }
        return buf.toString();
    }
}
