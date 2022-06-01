/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.concurrent.atomic.AtomicBoolean;

/** Flag for marking some aspect of widget or a toolkit representation 'dirty'.
 *
 *  @author Kay Kasemir
 */
public class DirtyFlag
{
    final private AtomicBoolean is_dirty;

    /** Construct initially dirty flag */
    public DirtyFlag()
    {
        this(true);
    }

    /** @param initially_set Initial state */
    public DirtyFlag(final boolean initially_set)
    {
        is_dirty = new AtomicBoolean(initially_set);
    }

    /** Mark as dirty */
    public void mark()
    {
        is_dirty.set(true);
    }

    /** @return Was dirty? */
    public boolean checkAndClear()
    {
        return is_dirty.getAndSet(false);
    }
}
