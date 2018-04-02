/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
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
package org.csstudio.scan.server.condition;

/** Interface to a condition on which one can wait
 *
 *  <p>A much simpler version of <code>java.util.concurrent.locks.Condition</code>
 *  that only offers <code>await</code>. No timeout etc.
 *
 *  @author Kay Kasemir
 */
public interface DeviceCondition
{
    /** Wait for the condition to be met
     *  @throws Exception on error
     */
    public void await() throws Exception;

    /** Force early completion.
     *
     *  <p>If one thread is currently inside <code>await</code>,
     *  another thread can call <code>complete</code>
     *  to trigger early return.
     *
     *  <p>If no thread is currently inside <code>await</code>,
     *  this call has no effect.
     */
    public void complete();

    // toString should give human-readable description,
    // including why the condition is currently waiting
}
