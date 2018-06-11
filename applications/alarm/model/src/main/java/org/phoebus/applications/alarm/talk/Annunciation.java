/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.talk;

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
public class Annunciation
{
    public final ObjectProperty<SeverityLevel> severity = new SimpleObjectProperty<>(SeverityLevel.OK);
    public final ObjectProperty<Instant> time_received = new SimpleObjectProperty<>(null);
    public final SimpleStringProperty message = new SimpleStringProperty();
    
    public Annunciation(Instant time_received, SeverityLevel severity, String message)
    {
        this.severity.set(severity);
        this.time_received.set(time_received);
        this.message.set(message);
    }
    
}
