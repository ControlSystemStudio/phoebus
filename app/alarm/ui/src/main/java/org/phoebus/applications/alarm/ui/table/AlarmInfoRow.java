/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import java.time.Instant;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.SeverityLevel;

import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;

/** Information for one 'row' in the alarm table
 *
 *  <p>Refers to an {@link AlarmClientLeaf} and duplicates
 *  its information in JFX observables.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmInfoRow
{
    /** Callback that obtains list of potentially changing properties
     *  for an alarm info row.
     *
     *  To be used with TableView and SortedList to re-sort the table
     *  when a property of an existing row changes.
     */
    public static final Callback<AlarmInfoRow, Observable[]> CHANGING_PROPERTIES =
    row -> new Observable[]
    {
        row.pv,
        row.description,
        row.severity,
        row.status,
        row.time,
        row.value,
        row.pv_severity,
        row.pv_status
    };

    public volatile AlarmClientLeaf item;
    public final StringProperty pv = new SimpleStringProperty();
    public final StringProperty description = new SimpleStringProperty();
    public final ObjectProperty<SeverityLevel> severity = new SimpleObjectProperty<>(SeverityLevel.OK);
    public final StringProperty status = new SimpleStringProperty();
    public final ObjectProperty<Instant> time = new SimpleObjectProperty<>(null);
    public final StringProperty value = new SimpleStringProperty();
    public final ObjectProperty<SeverityLevel> pv_severity = new SimpleObjectProperty<>(SeverityLevel.OK);
    public final StringProperty pv_status = new SimpleStringProperty();

    public AlarmInfoRow(final AlarmClientLeaf item)
    {
        this.item = item;

        pv.set(item.getName());

        // Remove the optional "*" (don't annunciate severity)
        // and "!" modifiers (always annunciate, don't summarize as "N more messages")
        // Also handle "*!".
        String desc = item.getDescription();
        if (desc.startsWith("*"))
            desc = desc.substring(1);
        if (desc.startsWith("!"))
            desc = desc.substring(1);
        description.set(desc);

        final ClientState state = item.getState();
        severity.set(state.severity);
        status.set(state.message);
        time.set(state.time);
        value.set(state.value);
        pv_severity.set(state.current_severity);
        pv_status.set(state.current_message);
    }

    public void copy(final AlarmInfoRow other)
    {
        this.item = other.item;
        pv.set(other.pv.get());
        description.set(other.description.get());
        severity.set(other.severity.get());
        status.set(other.status.get());
        time.set(other.time.get());
        value.set(other.value.get());
        pv_severity.set(other.pv_severity.get());
        pv_status.set(other.pv_status.get());
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();

        buf.append("PV: ").append(pv.get()).append("\n");
        buf.append("Description: ").append(description.get()).append("\n");
        buf.append("Alarm Time: ").append(time.get()).append("\n");
        buf.append("Alarm Severity: ").append(severity.get());
        buf.append(", Status: ").append(status.get());
        buf.append(", Value: ").append(value.get()).append("\n");
        buf.append("Current PV Severity: ").append(pv_severity.get());
        buf.append(", Status: ").append(pv_status.get()).append("\n");

        return buf.toString();
    }
}