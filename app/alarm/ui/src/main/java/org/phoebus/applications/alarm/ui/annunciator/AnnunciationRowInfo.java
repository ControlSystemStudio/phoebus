/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.time.Instant;

import org.phoebus.applications.alarm.model.SeverityLevel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Annunciation class for the Annunciation Table.
 * <p> Each annunciation contains a time, severity, and description represented by strings.
 * @author 1es
 *
 */
public class AnnunciationRowInfo implements Comparable<AnnunciationRowInfo>
{
    public final ObjectProperty<Instant> time_received = new SimpleObjectProperty<>(null);
    public final ObjectProperty<SeverityLevel> severity = new SimpleObjectProperty<>(SeverityLevel.OK);
    public final SimpleStringProperty message = new SimpleStringProperty();
    
    public AnnunciationRowInfo(Instant time_received, SeverityLevel severity, String message)
    {
        this.time_received.set(time_received);
        this.severity.set(severity);
        this.message.set(message);
    }

    /** 
     * Sort by severity, if severity is equal, sort by time received.
     */
    @Override
    public int compareTo(AnnunciationRowInfo other)
    {
        // Multiply by -1 to invert the sort order. The greater the severity, the greater the sort priority.
        int result = 0;
        if (! severity.isNull().get() && ! other.severity.isNull().get())
            result = -1 * this.severity.get().compareTo(other.severity.get());
        // Multiply by -1 to invert the sort order, oldest messages should be first.
        if (0 == result) 
            return -1 * this.time_received.get().compareTo(other.time_received.get());
        else
            return result;
    }
}
