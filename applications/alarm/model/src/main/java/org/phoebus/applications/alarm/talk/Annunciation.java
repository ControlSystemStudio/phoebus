/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.talk;

import gov.aps.jca.dbr.Severity;

/**
 * Annunciation class for the Annunciation Table.
 * <p> Each annunciation contains a time, severity, and description represented by strings.
 * @author 1es
 *
 */
public class Annunciation
{
    private final String time;
    private final Severity severity;
    private final String message;
    
    public Annunciation(final String time_received, Severity level, String msg)
    {
        time = time_received;
        severity = level;
        message = msg;
    }
    
    public String   getTime()     { return time; }
    public Severity getSeverity() { return severity; }
    public String   getMessage()  { return message; }
}
