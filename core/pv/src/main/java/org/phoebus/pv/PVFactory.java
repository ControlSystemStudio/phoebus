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
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public interface PVFactory
{
    /** ID of the Eclipse extension point for providing implementations */
    final public static String EXTENSION_POINT = "org.csstudio.vtype.pv.pvfactory";

    /** @return Type prefix that this PV factory supports */
    public String getType();

    /** Create a PV
     *
     *  @param name Full name of the PV as provided by user. May contain type prefix.
     *  @param base_name Base name of the PV, not including the prefix.
     *  @return PV
     *  @throws Exception on error
     */
    public PV createPV(final String name, final String base_name) throws Exception;

}
