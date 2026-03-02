/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.color;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/** Test helper: Delayed access to a file
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DelayedStream implements Callable<InputStream>
{
    private static final Logger logger = Logger.getLogger(DelayedStream.class.getName());

    private final CountDownLatch delay = new CountDownLatch(1);
    private final String filename;

    public DelayedStream(final String filename)
    {
        this.filename = filename;
    }

    @Override
    public InputStream call() throws Exception
    {
        logger.warning("Delaying file access.. on " + Thread.currentThread().getName());
        delay.await();
        logger.warning("Finally opening the file");
        return getClass().getResourceAsStream(filename);
    }

    public void proceed()
    {
        delay.countDown();
    }
}
