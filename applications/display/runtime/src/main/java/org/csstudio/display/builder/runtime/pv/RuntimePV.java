/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.logging.Level;

import org.phoebus.pv.PVListener;
import org.phoebus.pv.PVPool;
import org.phoebus.vtype.VType;

/** Process Variable, API for accessing life control system data.
 *
 *  <p>PVs are to be fetched from the {@link PVPool}
 *  and release to it when no longer used.
 *
 *  <p>The name of the PV is the name by which it was created.
 *  The underlying implementation might use a slightly different name.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public interface RuntimePV
{
    /** @return PV name */
    public String getName();

    /** Request notifications of PV updates.
     *
     *  <p>Note that the PV is shared via the {@link PVPool}.
     *  When updates are no longer desired, caller must
     *  <code>removeListener()</code>.
     *  Simply releasing the PV back to the {@link PVPool}
     *  will <b>not</b> automatically remove listeners!
     *
     *  @param listener Listener that will receive value updates
     *  @see #removeListener(PVListener)
     */
    public void addListener(final RuntimePVListener listener);

    /** @param listener Listener that will no longer receive value updates */
    public void removeListener(final RuntimePVListener listener);

    /** Read current value
     *
     *  <p>Should return the most recent value
     *  that listeners have received.
     *
     *  @return Most recent value of the PV. <code>null</code> if no known value.
     */
    public VType read();

    /** @return <code>true</code> if PV is read-only */
    public boolean isReadonly();

    /** Write value, no confirmation
     *  @param new_value Value to write to the PV
     *  @see RuntimePV#write(Object, PVWriteListener)
     *  @exception Exception on error
     */
    abstract public void write(final Object new_value) throws Exception;

    /** Legacy API that was accessed by some scripts
     *  @param new_value
     *  @throws Exception
     *  @Deprecated
     *  @see #write(Object)
     */
    @Deprecated
    default public void setValue(final Object new_value) throws Exception
    {
        if (! PVFactory.issued_write_warning)
        {
            PVFactory.issued_write_warning = true;
            // Called quite often for legacy displays, and display still works,
            // so don't log as WARNING
            logger.log(Level.INFO,
                    "Script calls 'setValue(" + new_value +") for PV '" + getName() +
                    "'. Update to 'write'");
        }
        write(new_value);
    }

    // Should provide PV name in toString() for debug messages that include the PV
    // @Override
    // public String toString()
}
