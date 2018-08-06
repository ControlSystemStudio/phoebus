/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.scanner;

/** Interface to something with a scheduled due time.
 *  Meant to mix with {@link Runnable}, but doesn't include
 *  runnable because something like a scan list might provide
 *  schedule information via a public interface, but
 *  no access to the <code>run()</code> method.
 *  @author Kay Kasemir
 */
public interface Scheduleable
{
    /** @return <code>true</code> if this runnable is due at all. */
    public boolean isDueAtAll();

    /** @return Next due time in system milliseconds.
     *  @throws Error when called without being due at all
     *  @see #isDueAtAll()
     */
    public long getNextDueTime() throws Error;
}
