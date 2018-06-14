/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import org.phoebus.applications.alarm.model.SeverityLevel;

/**
 * Annunciator Message class. 
 * Serves as a container for all the values the annunciator needs.
 * <p>
 * <b>Contains:</b>
 * <ol>
 * <li> standout - determines if the message should always be annunciated.
 * <li> severity - allows the messages to be easily sorted.
 * <li> message  - what the annunciator will say.
 * </ol>
 * @author Evan Smith
 **/
public class AnnunciatorMessage implements Comparable<AnnunciatorMessage>
{
    public final boolean       standout;
    public final SeverityLevel  severity;
    public final String         message;
    
    public AnnunciatorMessage(final boolean standout, final SeverityLevel severity, final String message)
    {
        this.standout = standout;
        this.severity = severity;
        this.message = message;
    }

    @Override
    public int compareTo(AnnunciatorMessage other)
    {
        return this.severity.compareTo(other.severity);
    }
}
