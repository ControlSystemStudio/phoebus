/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;

/** Test setup
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TestHelper
{
    @BeforeClass
    public static void configureLogging()
    {
        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.CONFIG);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(logger.getLevel());
    }
}
