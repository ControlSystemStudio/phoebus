/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** IO Utils
 *  @author Kay Kasemir
 */
public class IOUtils
{
    /** Copy from one stream to another
     *  @param input Stream to read, will be closed
     *  @param output Stream to write, will be closed
     *  @throws IOException on error
     */
    public static void copy(final InputStream input, final OutputStream output) throws IOException
    {
        final byte[] section = new byte[4096];
        int len;
        while ((len = input.read(section)) >= 0)
            output.write(section, 0, len);
        output.close();
        output.flush();
        input.close();
    }
}
