/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.spi;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.ArchiveReaders;

/** SPI for contributing an archive reader
 *
 *  <p>Contributions describe which type of URL
 *  they handle by providing the prefix,
 *  the start of the URL, which is checked against
 *  the actual URL.
 *
 *  <p>URLs in this case might be database URLs
 *  of the form "jdbc:subprotocol:...",
 *  i.e. not necessarily web URLs,
 *  so they are passed as strings.
 *
 *  @author Kay Kasemir
 */
public interface ArchiveReaderFactory
{
    /** Describe which type of URL this implementation handles.
     *
     *  <p>When creating an {@link ArchiveReader},
     *  the {@link ArchiveReaders} service
     *  checks if a URL starts with this prefix.
     *
     *  @return Prefix that this type of reader handles
     */
    public String getPrefix();

    /** Create reader
     *
     *  @param url URL that starts with prefix
     *  @return {@link ArchiveReader}
     *  @throws Exception on error
     */
    public ArchiveReader createReader(String url) throws Exception;
}
