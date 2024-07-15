/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.epics.vtype.VType;

/** Value Iterator
 *  @author Kay Kasemir
 */
public interface ValueIterator extends Iterator<VType>, Closeable
{
    @Override
    public default void close() throws IOException
    {
        // NOP
    }
}
