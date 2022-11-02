/*******************************************************************************
 * Copyright (c) 2017-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Set;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;
import org.phoebus.pv.PVPool.TypedName;
import org.phoebus.pv.ca.JCA_Preferences;

/** @author Kay Kasemir */
@SuppressWarnings("nls")
public class PVPoolTest
{
    @Test
    public void listPrefixes()
    {
        final Collection<String> prefs = PVPool.getSupportedPrefixes();
        System.out.println("Prefixes: " + prefs);
        assertThat(prefs, hasItem("ca"));
        assertThat(prefs, hasItem("sim"));
    }

    @Test
    public void analyzePVs()
    {
        TypedName type_name = TypedName.analyze("pva://ramp");
        assertThat(type_name.type, equalTo("pva"));
        assertThat(type_name.name, equalTo("ramp"));
        assertThat(type_name.toString(), equalTo("pva://ramp"));

        type_name = TypedName.analyze("ramp");
        assertThat(type_name.type, equalTo(PVPool.default_type));
        assertThat(type_name.name, equalTo("ramp"));
        assertThat(type_name.toString(), equalTo(PVPool.default_type + "://ramp"));
    }

    @Test
    public void equivalentPVs()
    {
        // Given "ramp" or "ca://ramp", all the other variants should be considered
        final String[] equivalent_pv_prefixes = new String[] { "ca", "pva" };
        Set<String> pvs = PVPool.getNameVariants("pva://ramp", equivalent_pv_prefixes);
        assertThat(pvs.size(), equalTo(3));
        assertThat(pvs, hasItem("ramp"));
        assertThat(pvs, hasItem("ca://ramp"));
        assertThat(pvs, hasItem("pva://ramp"));

        // For loc or sim which are not in the equivalent list, pass name through
        pvs = PVPool.getNameVariants("loc://ramp", equivalent_pv_prefixes);
        assertThat(pvs.size(), equalTo(1));
        assertThat(pvs, hasItem("loc://ramp"));

        pvs = PVPool.getNameVariants("sim://ramp", equivalent_pv_prefixes);
        assertThat(pvs.size(), equalTo(1));
        assertThat(pvs, hasItem("sim://ramp"));
    }


    @Test
    public void dumpPreferences() throws Exception
    {
        JCA_Preferences.getInstance();
        final Preferences prefs = Preferences.userNodeForPackage(PV.class);
        prefs.exportSubtree(System.out);
    }
}
