/*******************************************************************************
 * Copyright (c) 2013-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log;

/** Listener to a Data log
 *
 *  @author Kay Kasemir
 *  @author Eric Berryman - Original ScanContextListener that provided this information
 */
public interface DataLogListener
{
    /** Invoked by log if new log data has become available.
     *  @param datalog Log that has new data
     */
    public void logDataChanged(DataLog datalog);
}
