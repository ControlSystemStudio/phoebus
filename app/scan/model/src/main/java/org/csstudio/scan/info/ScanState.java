/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.info;

/** State of a scan
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public enum ScanState
{
    // Note:
    // The states used in here need to match the "State"
    // record of the scan.db in case that's used with the
    // scan server to indicate the state of the most recently
    // executed scan
    /** Scan is waiting to be executed */
    Idle("Idle", false, false),

    /** Scan is currently being executed */
    Running("Running", true, false),

    /** Scan was Running, currently paused */
    Paused("Paused", true, false),

    /** Scan was aborted by user */
    Aborted("Aborted", false, true),

    /** Scan failed because of an error */
    Failed("Failed", false, true),

    /** Scan ended normally, i.e. not aborted or failed */
    Finished("Finished - OK", false, true),

    /** Scan that executed in the past; data has been logged */
    Logged("Logged", false, true);

    final private String name;
    final private boolean active;
    final private boolean done;

    private ScanState(final String name, final boolean active, final boolean done)
    {
        this.name = name;
        this.active = active;
        this.done = done;
    }

    /** @return <code>true</code> if this is an 'active' state,
     *          representing a scan that's currently running (or paused)
     */
    public boolean isActive()
    {
        return active;
    }

    /** @return <code>true</code> if is a 'done' state,
     *          representing a scan that finished one way or another
     */
    public boolean isDone()
    {
        return done;
    }

    /** @return Human-readable representation */
    @Override
    public String toString()
    {
        return name;
    }
}
