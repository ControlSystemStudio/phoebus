/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandWithBody;

/** Listener to {@link Model}
 *
 *  <p>Will always be invoked on UI thread
 *
 *   @author Kay Kasemir
 */
public interface ModelListener
{
    /** Commands changed overall, need to refresh GUI */
    public void commandsChanged();

    /** @param parent <code>null</code> for root list of commands, otherwise parent of command
     *  @param command Command that was added */
    public void commandAdded(ScanCommandWithBody parent, ScanCommand command);

    /** @param command Command that was removed */
    public void commandRemoved(ScanCommand command);
}
