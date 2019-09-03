/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

/** PV Access structure with type ID
 *  @author Kay Kasemir
 */
abstract class PVADataWithID extends PVAData
{
    protected volatile short type_id = 0;

    protected PVADataWithID(final String name)
    {
        super(name);
    }

    /** @return Type ID, 0 when not set */
    public short getTypeID()
    {
        return type_id;
    }

    /** @param id Type ID, 0 to clear */
    public void setTypeID(final short id)
    {
        this.type_id = id;
    }
}
