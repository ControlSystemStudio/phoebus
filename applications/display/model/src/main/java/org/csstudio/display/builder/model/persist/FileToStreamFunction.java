/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import java.io.InputStream;

/** Function that opens a stream
 *
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface FileToStreamFunction
{
    /** Open file
     *  @param filename Filename to open
     *  @return Stream
     *  @throws Exception on error
     */
    public InputStream open(final String filename) throws Exception;
}
