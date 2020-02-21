/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ServiceLoader;

import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.spi.PVProposalProvider;
import org.phoebus.framework.workbench.WorkbenchPreferences;

/** Autocompletion Service for PVs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVProposalService extends ProposalService
{
    public static final PVProposalService INSTANCE = new PVProposalService();

    private PVProposalService()
    {
        // Enable built-in proposal providers
        final PreferencesReader prefs = new PreferencesReader(WorkbenchPreferences.class, "/autocomplete_preferences.properties");
        if (prefs.getBoolean("enable_loc_pv_proposals"))
            providers.add(LocProposalProvider.INSTANCE);
        if (prefs.getBoolean("enable_sim_pv_proposals"))
            providers.add(SimProposalProvider.INSTANCE);
        if (prefs.getBoolean("enable_sys_pv_proposals"))
            providers.add(SysProposalProvider.INSTANCE);
        if (prefs.getBoolean("enable_pva_pv_proposals"))
            providers.add(PvaProposalProvider.INSTANCE);
        if (prefs.getBoolean("enable_mqtt_pv_proposals"))
            providers.add(MqttProposalProvider.INSTANCE);
        if (prefs.getBoolean("enable_formula_proposals"))
            providers.add(FormulaProposalProvider.INSTANCE);

        // Use SPI to add site-specific PV name providers
        for (PVProposalProvider add : ServiceLoader.load(PVProposalProvider.class))
        {
            logger.config("Adding PV Proposal Provider '" + add.getName() + "'");
            providers.add(add);
        }
    }
}
