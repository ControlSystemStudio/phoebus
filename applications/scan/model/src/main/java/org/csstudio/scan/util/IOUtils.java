/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** I/O utils, using some method names from Apache commons-io
 *  in case that's used later.
 *  @author Kay Kasemir
 */
public class IOUtils
{
    /** Read text from stream
     *  @param stream {@link InputStream} to read
     *  @return Text read from the stream
     *  @throws IOException on error
     */
    public static String toString(final InputStream stream) throws IOException
    {
        return new String(toByteArray(stream));
    }

    /** Read bytes from stream
     *  @param stream {@link InputStream} to read
     *  @return bytes read from stream
     *  @throws IOException on error
     */
    public static byte[] toByteArray(final InputStream stream) throws IOException
    {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        org.phoebus.framework.util.IOUtils.copy(stream, result);
        return result.toByteArray();
    }
}
