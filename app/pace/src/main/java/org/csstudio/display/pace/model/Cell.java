/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.model;


import java.text.MessageFormat;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.csstudio.display.pace.Messages;
import org.epics.vtype.VType;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.util.time.TimestampFormats;
import org.phoebus.core.vtypes.VTypeHelper;

import io.reactivex.disposables.Disposable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

/** One cell in the model.
 *
 *  <p>Knows about the Instance and Column where this cell resides,
 *  connects to a PV, holds the most recent value of the PV
 *  as well as an optional user value that overrides the PV's value.
 *
 *  <p>In addition, a cell might have "meta PVs" that contain the name
 *  of the user, date, and a comment regarding the last change
 *  of the "main" PV.
 *
 *  @author Kay Kasemir
 *  @author Delphy Nypaver Armstrong
 */
@SuppressWarnings("nls")
public class Cell
{
    final private Instance instance;

    final private Column column;

    /** Name of the cell's PV.
     *  Matches pv.getName() except for possible 'decoration'
     *  like "ca://" that the PV adds.
     */
    final private String pv_name;

    /** Control system PV for 'live' value */
    private PV pv;
    private Disposable pv_subscription;

    /** Most recent value received from PV */
    private volatile String current_value = null;

    /** Value that the user entered. */
    private volatile String user_value = null;

    /** Optional PVs for the name of the person who made the last change,
     *  the date of the change, and a comment.
     *  Either may be <code>null</code>
     */
    private PV last_name_pv, last_date_pv, last_comment_pv;
    private Disposable last_name_subscription, last_date_subscription, last_comment_subscription;
    private volatile String last_name = null, last_date = null, last_comment = null;

    final private SimpleStringProperty gui_value = new SimpleStringProperty(Messages.UnknownValue);

    /** Initialize
     *  @param instance Instance (row) that holds this cell
     *                  and provides the macro substitutions for the cell
     *  @param column   Column that holds this cell
     *                  and provides the macro-ized PV name
     *                  for all cells in the column
     *  @throws Exception on error in macros
     */
    public Cell(final Instance instance, final Column column) throws Exception
    {
        this.instance = instance;
        this.column = column;
        pv_name = MacroHandler.replace(instance.getMacros(), column.getPvWithMacros());
    }

    /** @return Instance (row) that contains this cell */
    public Instance getInstance()
    {
        return instance;
    }

    /** @return Column that contains this cell */
    public Column getColumn()
    {
        return column;
    }

    /** @return <code>true</code> for read-only cell */
    public boolean isReadOnly()
    {
        return pv != null  &&  column.isReadonly();
    }

    /** Even though a cell may be configured as writable,
     *  the underlying PV might still prohibit write access.
     *  @return <code>true</code> for PVs that can be written.
     */
    public boolean isPVWriteAllowed()
    {
        return pv != null  && !pv.isReadonly();
    }

    /** If the user entered a value, that's it.
     *  Otherwise it's the PV's value, or UNKNOWN
     *  if we have nothing.
     *  @return Value of this cell
     */
    public ObservableValue<String> getObservable()
    {
        if (user_value != null)
            gui_value.set(user_value);
        else if (current_value != null)
            gui_value.set(current_value);
        else
            gui_value.set(Messages.UnknownValue);
        return gui_value;
    }

    /** @return Original value of PV or <code>null</code>
     */
    public String getCurrentValue()
    {
        return current_value;
    }

    /** Set a user-specified value.
     *  <p>
     *  If this value matches the PV's value, we revert to the PV's value.
     *  Otherwise this defines a new value that the user entered to
     *  replace the original value of the PV.
     *  @param value Value that the user entered for this cell
     */
    public void setUserValue(final String value)
    {
        if (value.equals(current_value))
            user_value = null;
        else
            user_value = value;
        instance.getModel().fireCellUpdate(this);
    }

    /** @return Value that user entered to replace the original value,
     *          or <code>null</code>
     */
    public String getUserValue()
    {
        return user_value;
    }

    private String original_pv_value = null;
    private String original_name_value = null;
    private String original_date_value = null;

    /** Save value entered by user to PV
     *
     *  <p>On success, this should be followed by a call to <code>clearUserValue</code>,
     *  or rolled back via a call to <code>revertOriginalValue</code>
     *
     *  @param user_name Name of the user to be logged for cells with
     *                   a last user meta PV
     *  @throws Exception on error
     */
    public void saveUserValue(final String user_name) throws Exception
    {
        if (!isEdited())
            return;
        if (pv != null)
        {
            if (! isPVWriteAllowed())
                throw new Exception(pv.getName() + " is read-only");
            original_pv_value = current_value;
            pv.write(user_value);
        }
        if (last_name_pv != null)
        {
            original_name_value = last_name;
            last_name_pv.write(user_name);
        }
        if (last_date_pv != null)
        {
            original_date_value = last_date;
            last_date_pv.write(TimestampFormats.SECONDS_FORMAT.format(Instant.now()));
        }
    }

    /** Revert to the state before a user value was saved
     *  @throws Exception on error
     */
    public void revertOriginalValue() throws Exception
    {
        if (pv != null  &&  original_pv_value != null)
            pv.write(original_pv_value);
        if (last_name_pv != null  &&  original_name_value != null)
            last_name_pv.write(original_name_value);
        if (last_date_pv != null  &&  original_date_value != null)
            last_date_pv.write(original_date_value);
    }

    /** Clear a user-specified value */
    public void clearUserValue()
    {
        user_value = null;
        original_pv_value = null;
        original_name_value = null;
        original_date_value = null;
        instance.getModel().fireCellUpdate(this);
    }

    /** @return <code>true</code> if user entered a value */
    public boolean isEdited()
    {
        return user_value != null;
    }


    /** @return <code>true</code> if the cell has meta information about
     *  the last change
     *  @see #getLastComment()
     *  @see #getLastDate()
     *  @see #getLastUser()
     */
    public boolean hasMetaInformation()
    {
        return last_name_pv != null || last_date_pv != null ||
              last_comment_pv != null;
    }

    /** @return User name for last change to the main PV */
    public String getLastUser()
    {
        return getOptionalValue(last_name);
    }

    /** @return Date of last change to the main PV */
    public String getLastDate()
    {
        return getOptionalValue(last_date);
    }

    /** @return Comment for last change to the main PV */
    public String getLastComment()
    {
        return getOptionalValue(last_comment);
    }

    /** Get value of optional PV
     *  @param optional_pv PV to check, may be <code>null</code>
     *  @return Last value, never <code>null</code>
     */
    private String getOptionalValue(final String string)
    {
        if (string == null  ||  string.isEmpty())
            return Messages.UnknownValue;
        return string;
    }

    final long THROTTLE_MS = 250;

    /** Start the PV connection */
    public void start() throws Exception
    {
        //  Create the main PV and add listener
        if (pv_name.length() <= 0)
            pv = null;
        else
        {
            pv = PVPool.getPV(pv_name);
            pv_subscription = pv.onValueEvent()
                                .throttleLatest(THROTTLE_MS, TimeUnit.MILLISECONDS)
                                .subscribe(this::updateValue);
        }

        // Create the optional comment pvs.
        String name = MacroHandler.replace(instance.getMacros(), column.getNamePvWithMacros());
        if (name.length() <= 0)
            last_name_pv = null;
        else
        {
            last_name_pv = PVPool.getPV(name);
            last_name_subscription = last_name_pv.onValueEvent()
                                                 .throttleLatest(THROTTLE_MS, TimeUnit.MILLISECONDS)
                                                 .subscribe(this::updateLastName);

        }
        name = MacroHandler.replace(instance.getMacros(), column.getDatePvWithMacros());
        if (name.length() <= 0)
            last_date_pv = null;
        else
        {
            last_date_pv = PVPool.getPV(name);
            last_date_subscription = last_date_pv.onValueEvent()
                                                 .throttleLatest(THROTTLE_MS, TimeUnit.MILLISECONDS)
                                                 .subscribe(this::updateLastDate);
        }

        name = MacroHandler.replace(instance.getMacros(), column.getCommentPvWithMacros());
        if (name.length() <= 0)
            last_comment_pv = null;
        else
        {
            last_comment_pv = PVPool.getPV(name);
            last_comment_subscription = last_comment_pv.onValueEvent()
                                                       .throttleLatest(THROTTLE_MS, TimeUnit.MILLISECONDS)
                                                       .subscribe(this::updateLastComment);
        }
    }

    private void updateValue(final VType value)
    {
        current_value = VTypeHelper.toString(value);
        instance.getModel().fireCellUpdate(this);
    }

    private void updateLastName(final VType value)
    {
        last_name = VTypeHelper.toString(value);
        instance.getModel().fireCellUpdate(this);
    }

    private void updateLastDate(final VType value)
    {
        last_date = VTypeHelper.toString(value);
        instance.getModel().fireCellUpdate(this);
    }

    private void updateLastComment(final VType value)
    {
        last_comment = VTypeHelper.toString(value);
        instance.getModel().fireCellUpdate(this);
    }

    /** Stop the PV connection */
    public void stop()
    {
        if (last_comment_pv != null)
        {
            last_comment_subscription.dispose();
            PVPool.releasePV(last_comment_pv);
            last_comment_pv = null;
        }
        if (last_date_pv != null)
        {
            last_date_subscription.dispose();
            PVPool.releasePV(last_date_pv);
            last_date_pv = null;
        }
        if (last_name_pv != null)
        {
            last_name_subscription.dispose();
            PVPool.releasePV(last_name_pv);
            last_name_pv = null;
        }
        if (pv != null)
        {
            pv_subscription.dispose();
            PVPool.releasePV(pv);
            pv = null;
        }
    }

    /** @return PV name */
    public String getName()
    {
        return pv_name;
    }

    /** @return Name of comment PV or "" */
    public String getCommentPVName()
    {
        if (last_comment_pv == null)
            return ""; //$NON-NLS-1$
        return last_comment_pv.getName();
    }

    /** @return Info for e.g. tool tip */
    public String getInfo()
    {
        final StringBuilder buf = new StringBuilder();

        // Creating basic PV name, value tooltip
        buf.append(MessageFormat.format(Messages.InstanceLabelProvider_PVValueFormat, getName(), getObservable().getValue()));
        // Extend if 'edited'
        if (isEdited())
            buf.append(Messages.InstanceLabelProvider_OrigAppendix).append(getCurrentValue());
        // Extend if 'read-only'
        if (isReadOnly() || !isPVWriteAllowed())
            buf.append(Messages.InstanceLabelProvider_ReadOnlyAppendix);
        // If the cell has meta information, add the person who made the change,
        // the date of the change and the comment to the tool-tip.
        if (hasMetaInformation())
            buf.append(MessageFormat.format(Messages.InstanceLabelProvider_PVCommentTipFormat,
                                            getLastUser(),
                                            getLastDate(),
                                            getLastComment()));

        return buf.toString();
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        return "Cell " + pv_name + " = " + getObservable().getValue();
    }
}
