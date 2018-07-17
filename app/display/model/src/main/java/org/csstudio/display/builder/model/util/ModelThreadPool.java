/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/** Thread pool for model related operations
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelThreadPool
{
    private static final ExecutorService executor = NamedDaemonPool.createThreadPool("DisplayModel");

    private static final ScheduledExecutorService timer = NamedDaemonPool.createTimer("DisplayModelTimer");

    /** @return {@link ExecutorService} for thread pool meant for model related background tasks */
    public static ExecutorService getExecutor()
    {
        return executor;
    }

    /** @return {@link ScheduledExecutorService} for thread pool meant for model related timer tasks */
    public static ScheduledExecutorService getTimer()
    {
        return timer;
    }
}
