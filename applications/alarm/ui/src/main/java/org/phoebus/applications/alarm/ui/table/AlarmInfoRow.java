/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import org.phoebus.applications.alarm.model.SeverityLevel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Information for one 'row' in the alarm table
 *  @author Kay Kasemir
 */
public class AlarmInfoRow
{
    final StringProperty pv = new SimpleStringProperty();
    final ObjectProperty<SeverityLevel> severity = new SimpleObjectProperty<>(SeverityLevel.OK);

    public AlarmInfoRow(final String pv, final SeverityLevel severity)
    {
        this.pv.set(pv);
        this.severity.set(severity);
    }
}