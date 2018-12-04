/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.model;

/** Listener to Model changes
 *  @author Kay Kasemir
 */
public interface ModelListener
{
    /** Notification of cell update.
     *  @param cell Cell that changed its value in any way:
     *              Received new data from PV,
     *              user updated the value, reset to original value, ...
     */
    void cellUpdate(Cell cell);
}
