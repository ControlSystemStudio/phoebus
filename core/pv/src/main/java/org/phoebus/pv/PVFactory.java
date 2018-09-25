/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

/** Factory for creating {@link PV}s
 *
 *  <p>Code that needs to create a {@link PV}
 *  does this via the {@link PVPool}.
 *
 *  <p>Each type of {@link PV} provides a factory
 *  for creating that type of PV and registers it
 *  with the {@link PVPool}.
 *
 *  <p>A full PV name can have the form <code>type://name&lt;type&gt;(params)</code>,
 *  for example <code>loc://x(42.3)</code>
 *  or <code>loc://choices&lt;VEnum&gt;(2, "A", "B", "C")</code>.
 *
 *  <p>The 'base name' is everything after the "type://" prefix.
 *  The 'core name' is used to uniquely identify the name in the PV pool.
 *  For most PVs, that is the same as the complete name.
 *  For local PVs, the core name is just for example "loc://choices" without the initialization
 *  parameters.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public interface PVFactory
{
    /** ID of the Eclipse extension point for providing implementations */
    final public static String EXTENSION_POINT = "org.csstudio.vtype.pv.pvfactory";

    /** @return Type prefix that this PV factory supports */
    public String getType();

    /** Determine core name
     *  @param name Name of the PV
     *  @return Core name that uniquely identifies the PV
     */
    public default String getCoreName(final String name)
    {
        return name;
    }

    /** Create a PV
     *
     *  @param name Full name of the PV as provided by user. May contain type prefix and parameters.
     *  @param base_name Base name of the PV, not including the prefix.
     *  @return PV
     *  @throws Exception on error
     */
    public PV createPV(final String name, final String base_name) throws Exception;
}
