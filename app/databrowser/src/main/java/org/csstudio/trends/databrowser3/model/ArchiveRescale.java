/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import org.csstudio.trends.databrowser3.Messages;

/** How to handle newly received archived data?
 *  @author Kay Kasemir
 */
public enum ArchiveRescale
{
    /** Keep display as is */
    NONE(Messages.ArchiveRescale_NONE),
    /** Perform stagger */
    STAGGER(Messages.ArchiveRescale_STAGGER);

    final private String name;

    private ArchiveRescale(final String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
