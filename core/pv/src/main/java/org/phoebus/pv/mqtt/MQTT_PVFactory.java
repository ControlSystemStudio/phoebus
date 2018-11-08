/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.mqtt;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

/** MQTT implementation of org.phoebus.pv.PVFactory.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MQTT_PVFactory implements PVFactory
{
    final public static String TYPE = "mqtt";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception
    {
       return new MQTT_PV(name, base_name);
    }
}
