/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.talk;

/**
 * Listener for {@link TalkClient}
 * @author Evan Smith
 *
 */
public interface TalkClientListener
{
    /**
     * Called when client receives a message.
     * @param severity Alarm Severity
     * @param description Description attached to alarm
     */
    public void messageRecieved(final String severity, final String description);
}
