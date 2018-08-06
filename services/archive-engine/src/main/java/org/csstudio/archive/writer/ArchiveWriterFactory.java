/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer;

import org.csstudio.archive.Preferences;
import org.csstudio.archive.writer.rdb.RDBArchiveWriter;

/** Factory for obtaining an {@link ArchiveWriter}
 *  @author Kay Kasemir
 */
public class ArchiveWriterFactory
{
    /** Obtain archive writer interface from plugin registry
     *  @return {@link ArchiveWriter}
     *  @throws Exception on error: No implementation found, or error initializing it
     */
    public static ArchiveWriter getArchiveWriter() throws Exception
    {
        // XXX Use SPI when there's more than one implementation.
        return new RDBArchiveWriter(Preferences.url, Preferences.user, Preferences.password, Preferences.schema, Preferences.use_array_blob);
    }
}
