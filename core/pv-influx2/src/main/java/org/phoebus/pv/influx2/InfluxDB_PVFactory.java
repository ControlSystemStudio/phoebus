/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.influx2;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

/**
 * Factory class for creating {@link InfluxDB_PV} instances.
 * <p>
 * This factory integrates with the Phoebus PV framework and registers the
 * "influx" PV type so it can be discovered and instantiated dynamically
 * when a PV URI starts with {@code influx2://}.
 *
 */
public class InfluxDB_PVFactory implements PVFactory {
    /** The PV type identifier used to register this factory. */
    public static final String TYPE = "influx2";

    /**
     * Returns the PV type string handled by this factory.
     * This value is used to match PVs that should be created by this factory.
     *
     * @return the PV type string ("influx")
     */
    @Override
    public String getType() { return TYPE; }

    /**
     * Creates a new {@link InfluxDB_PV} instance with the given name and base_name.
     *
     * @param name      the full PV name
     * @param base_name the base URI used to extract the InfluxDB bucket and measurement
     * @return a new {@code InfluxDB_PV} instance
     */
    @Override
    public PV createPV(String name, String base_name) { return new InfluxDB_PV(name, base_name); }
}
