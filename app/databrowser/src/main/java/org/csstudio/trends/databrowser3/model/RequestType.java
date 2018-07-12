/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import org.csstudio.trends.databrowser3.Messages;

/** Archive data request types
 *  @author Kay Kasemir
 */
public enum RequestType
{
    /** If possible, get the raw data.
     *  <p>
     *  This was added at the request of DESY.
     *  Ordinarily, the OPTIMIZED request should
     *  provide RAW data automatically once we zoom
     *  in far enough, so all you get from this
     *  request is a waste of memory.
     */
    RAW(Messages.Request_raw),

    /** If possible, get data optimized for plotting. */
    OPTIMIZED(Messages.Request_optimized);

    final private String name;

    private RequestType(String name)
    {
        this.name = name;
    }

    /** @return Localized name of this request type */
    @Override
    public String toString()
    {
        return name;
    }
}
