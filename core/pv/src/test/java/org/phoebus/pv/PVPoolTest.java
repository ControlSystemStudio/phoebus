package org.phoebus.pv;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.prefs.Preferences;

import org.junit.Test;
import org.phoebus.pv.ca.JCA_Preferences;

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
    public void dumpPreferences() throws Exception
    {
        JCA_Preferences.getInstance();
        final Preferences prefs = Preferences.userNodeForPackage(PV.class);
        prefs.exportSubtree(System.out);
    }
}
