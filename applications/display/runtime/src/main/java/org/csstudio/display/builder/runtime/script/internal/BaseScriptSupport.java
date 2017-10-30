/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script.internal;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Base for a specific (Python, Jython, ..) script support
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BaseScriptSupport
{
    /** Scripts that have been submitted, awaiting execution, to avoid queuing them again.
    *
    *  <p>Relies on the fact that each script is unique identified by the {@link Script} itself,
    *  they're not submitted with different widget and pvs parameters.
    */
    // Map with bool value (always true) is used as a Set.
    // Could use Map with AtomicInteger to count queued invocations.
    private final ConcurrentHashMap<Script, Boolean> queued_scripts = new ConcurrentHashMap<>();

    /** Note that script is to-be-executed
     *  @param script {@link Script}
     *  @return <code>true</code> to proceed, <code>false</code> if that script is already about to be executed
     */
    public boolean markAsScheduled(final Script script)
    {
        final Boolean was_queued = queued_scripts.putIfAbsent(script, Boolean.TRUE);
        if (was_queued == Boolean.TRUE)
        {
            logger.log(Level.FINE, "Skipping script {0}, already queued for execution", script);
            return false;
        }
        return true;
    }

    /** @param script Script that's being executed, OK to schedule again */
    public void removeScheduleMarker(final Script script)
    {
        queued_scripts.remove(script);
    }
}
