/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script.internal;

import java.util.concurrent.Future;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.pv.RuntimePV;

/** Compiled script (Jython, JavaScript)
 *  @author Kay Kasemir
 */
public interface Script
{
    /** Submit script for execution.
     *
     *  <p>Associated script support maintains the thread which
     *  executes all scripts within that support instance.
     *
     *  <p>Caller may use Future to await end of script execution,
     *  or continue while script is queued for execution.
     *
     *  @param widget Widget for the script's context
     *  @param pvs PVs for the script's context
     *  @return Future for awaiting end of execution
     */
    public Future<Object> submit(Widget widget, RuntimePV... pvs);
}
