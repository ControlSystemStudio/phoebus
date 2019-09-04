/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.formula;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

/** Factory for creating {@link FormulaPV}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaPVFactory implements PVFactory
{
    final public static String TYPE = "eq";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public PV createPV(final String name, final String base_name) throws Exception
    {
        return new FormulaPV(name, base_name);
    }
}
