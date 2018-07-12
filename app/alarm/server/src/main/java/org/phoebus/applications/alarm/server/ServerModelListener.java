/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

/** Listener to the server model
 *  @author Kay Kasemir
 */
public interface ServerModelListener
{
    /** @param path Alarm tree node
     *  @param json Content of the command
     */
    public void handleCommand(String path, String json);
}
