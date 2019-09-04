/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.phoebus.pv.PV;

/** Base for simulated PVs
 *
 *  @author Kay Kasemir, based on similar code in org.csstudio.utility.pv and diirt
 */
@SuppressWarnings("nls")
abstract public class SimulatedPV extends PV
{
    /** Timer for periodic updates */
    private final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, target ->
    {
        final Thread thread = new Thread(target, "SimPV");
        thread.setDaemon(true);
        return thread;
    });

    /** Task that was submitted for periodic updates */
    private ScheduledFuture<?> task;

    /** @param name Full PV name */
    public SimulatedPV(final String name)
    {
        super(name);

        // Simulated PVs are read-only
        notifyListenersOfPermissions(true);
    }

    /** Start periodic updates
     *  @param update_seconds Update period in seconds
     */
    protected void start(final double update_seconds)
    {
        // Limit rate to 100 Hz
        final long milli = Math.round(Math.max(update_seconds, 0.01) * 1000);
        task = executor.scheduleAtFixedRate(this::update, milli, milli, TimeUnit.MILLISECONDS);
    }

    /** Called by periodic timer */
    abstract protected void update();

    @Override
    protected void close()
    {
        if (! task.cancel(false))
            logger.log(Level.WARNING, "Cannot cancel updates for " + getName());
        super.close();
    }
}
