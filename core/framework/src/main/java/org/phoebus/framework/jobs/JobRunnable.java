/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.jobs;

/** Runnable for a {@link Job}
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface JobRunnable
{
    /** @param monitor Monitor for reporting progress
     *  @throws Exception on error
     */
    public void run(JobMonitor monitor) throws Exception;
}
