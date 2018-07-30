/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer;

/** Interface for a channel to which samples can be written
 *
 *  @see ArchiveWriter
 *
 *  @author Kay Kasemir
 */
public interface WriteChannel
{
    /** @return Name of the channel */
    public String getName();
}
