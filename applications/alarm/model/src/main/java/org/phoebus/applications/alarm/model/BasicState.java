/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

/** Basic state held by every node in the alarm hierarchy
 *  @author Kay Kasemir
 */
public class BasicState
{
    public final SeverityLevel severity;

    public BasicState(final SeverityLevel severity)
    {
        this.severity = severity;
    }

    /** @return Severity level of alarm. <code>null</code> if dynamically disabled */
    public SeverityLevel getSeverity()
    {
        return severity;
    }

    @Override
    public int hashCode()
    {
        return (severity == null) ? 0 : severity.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof BasicState))
            return false;
        final BasicState other = (BasicState) obj;
        return severity == other.severity;
    }
}
