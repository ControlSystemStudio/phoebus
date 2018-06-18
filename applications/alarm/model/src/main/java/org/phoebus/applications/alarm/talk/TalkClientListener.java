/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.talk;

import org.phoebus.applications.alarm.model.SeverityLevel;

/**
 * Interface for talk client listener.
 * @author Evan Smith
 *
 */
public interface TalkClientListener
{
    public void messageReceived(SeverityLevel severity, String message);
}
