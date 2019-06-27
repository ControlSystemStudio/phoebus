/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

/** Monitor
 *
 *  <p>Handle for a subscription,
 *  obtained from {@link PVAChannel#subscribe()}.
 *
 *  @author Kay Kasemir
 */
public interface Monitor extends AutoCloseable
{
    /** Cancel the subscription, i.e. stop receiving updates */
    @Override
    public void close() throws Exception;
}
