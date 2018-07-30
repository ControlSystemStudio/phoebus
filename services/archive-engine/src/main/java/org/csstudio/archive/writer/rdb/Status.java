/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

/** Status ID
 *  @author Kay Kasemir
 */
public class Status extends StringID
{
    public Status(int id, String name)
    {
        super(id, name);
    }

    @Override
    @SuppressWarnings("nls")
    final public String toString()
    {
        return String.format("Status '%s' (%d)", getName(), getId());
    }
}
