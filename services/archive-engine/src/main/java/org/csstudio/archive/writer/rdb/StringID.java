/*******************************************************************************
 * Copyright (c) 2010-2818 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

/** A name (string) with an ID.
 *  @author Kay Kasemir
 */
public class StringID
{
    final private int id;
    final private String name;

    /** Construct name with given ID */
    public StringID(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    /** @return ID */
    final public int getId()
    {
        return id;
    }

    /** @return name */
    final public String getName()
    {
        return name;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "'" + name + "' (" + id + ")";
    }
}
