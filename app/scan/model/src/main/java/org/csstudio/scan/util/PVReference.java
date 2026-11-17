/*******************************************************************************
 * Copyright (c) 2024 Fermi National Accelerator Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.util;

/** Wrapper that marks a string as a PV name to be read at scan execution time.
 *
 *  <p>When a scan command's {@code desired_value} is a {@link PVReference},
 *  the scan server will read the current value of the named PV at runtime
 *  and use that value as the comparison target.
 *
 *  <p>In XML scan files, an unquoted, non-numeric {@code <value>} element
 *  (e.g. {@code <value>some:pv:name</value>}) is parsed as a {@link PVReference}.
 *  A quoted string (e.g. {@code <value>"hello"</value>}) remains a literal
 *  {@link String} used for text comparison.
 *
 *  @author Fermi Controls
 */
public class PVReference
{
    private final String pv_name;

    /** @param pv_name Name of the PV whose value will be read at runtime */
    public PVReference(final String pv_name)
    {
        this.pv_name = pv_name;
    }

    /** @return PV name */
    public String getPVName()
    {
        return pv_name;
    }

    @Override
    public String toString()
    {
        return pv_name;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (this == other)
            return true;
        if (!(other instanceof PVReference))
            return false;
        return pv_name.equals(((PVReference) other).pv_name);
    }

    @Override
    public int hashCode()
    {
        return pv_name.hashCode();
    }
}
