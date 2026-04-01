/*******************************************************************************
 * Copyright (c) 2025 Lawrence Berkeley National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for PropertyPreferenceLoader macro replacement
 *
 *  <p>Verifies that $(phoebus.install), $(phoebus.user), and $(user.home)
 *  macro replacements only encode the resolved path portion,
 *  not the entire preference value.
 *
 *  @author Conor Schofield
 */
@SuppressWarnings("nls")
public class PropertyPreferenceLoaderTest
{
    private String origPhoebusInstall;
    private String origPhoebusUser;
    private String origUserHome;

    @BeforeEach
    public void saveSystemProperties()
    {
        origPhoebusInstall = System.getProperty("phoebus.install");
        origPhoebusUser = System.getProperty("phoebus.user");
        origUserHome = System.getProperty("user.home");
    }

    @AfterEach
    public void restoreSystemProperties()
    {
        restoreProperty("phoebus.install", origPhoebusInstall);
        restoreProperty("phoebus.user", origPhoebusUser);
        restoreProperty("user.home", origUserHome);

        // Clean up test preferences
        try
        {
            final Preferences prefs = Preferences.userRoot().node("/org/phoebus/test");
            prefs.removeNode();
        }
        catch (Exception e)
        {
            // ignore
        }
    }

    private void restoreProperty(final String key, final String value)
    {
        if (value == null)
            System.clearProperty(key);
        else
            System.setProperty(key, value);
    }

    /** Load a single property and return its stored preference value */
    private String loadAndGet(final String propertyLine) throws Exception
    {
        final ByteArrayInputStream stream = new ByteArrayInputStream(
                propertyLine.getBytes(StandardCharsets.UTF_8));
        PropertyPreferenceLoader.load(stream);
        return Preferences.userRoot().node("/org/phoebus/test").get("value", "");
    }

    @Test
    public void testPhoebusInstallMacroEncodesOnlyPath() throws Exception
    {
        // Path with spaces — encoding should apply only to this path
        System.setProperty("phoebus.install", "/opt/my phoebus");

        final String value = loadAndGet(
                "org.phoebus.test/value=$(phoebus.install)/config/style.pdf?app=web, Style Guide");

        // The resolved path should have %20 for spaces
        assertTrue(value.contains("/opt/my%20phoebus/config/style.pdf"),
                "Resolved path should have spaces encoded: " + value);
        // The display name should NOT have %20
        assertFalse(value.contains("Style%20Guide"),
                "Display name spaces should not be encoded: " + value);
        assertThat(value, equalTo("/opt/my%20phoebus/config/style.pdf?app=web, Style Guide"));
    }

    @Test
    public void testPhoebusInstallInMultiValuePreference() throws Exception
    {
        // Simulates the top_resources format with pipe-delimited entries
        System.setProperty("phoebus.install", "/opt/my phoebus");

        final String value = loadAndGet(
                "org.phoebus.test/value=pv://?&app=probe,PV Probe Tool | https://example.com?app=web, Web App | $(phoebus.install)/config/guide.pdf?app=web, Style Guide");

        // Spaces in display names should be preserved
        assertFalse(value.contains("PV%20Probe"),
                "Display names before macro should not be encoded: " + value);
        assertFalse(value.contains("Web%20App"),
                "Display names in other entries should not be encoded: " + value);
        assertFalse(value.contains("Style%20Guide"),
                "Display name after macro should not be encoded: " + value);

        // Spaces around pipe delimiters should be preserved
        assertTrue(value.contains(" | "),
                "Pipe delimiters should keep surrounding spaces: " + value);

        // Only the resolved install path should be encoded
        assertTrue(value.contains("my%20phoebus"),
                "Spaces in resolved install path should be encoded: " + value);

        // The pv:// and https:// URIs should NOT have %20 prefixes
        assertFalse(value.contains("%20pv://"),
                "pv:// scheme should not get %20 prefix: " + value);
        assertFalse(value.contains("%20https://"),
                "https:// scheme should not get %20 prefix: " + value);
    }

    @Test
    public void testPhoebusInstallPathWithoutSpaces() throws Exception
    {
        // Path without spaces — nothing should change beyond macro substitution
        System.setProperty("phoebus.install", "/opt/phoebus");

        final String value = loadAndGet(
                "org.phoebus.test/value=$(phoebus.install)/config/guide.pdf?app=web, Style Guide");

        assertThat(value, equalTo("/opt/phoebus/config/guide.pdf?app=web, Style Guide"));
    }

    @Test
    public void testPhoebusUserMacroEncodesOnlyPath() throws Exception
    {
        System.setProperty("phoebus.user", "/home/my user/.phoebus");

        final String value = loadAndGet(
                "org.phoebus.test/value=$(phoebus.user)/layouts/default.xml, Default Layout");

        assertTrue(value.contains("my%20user"),
                "Spaces in resolved user path should be encoded: " + value);
        assertFalse(value.contains("Default%20Layout"),
                "Display name should not be encoded: " + value);
    }

    @Test
    public void testUserHomeMacroEncodesOnlyPath() throws Exception
    {
        System.setProperty("user.home", "/home/my user");

        final String value = loadAndGet(
                "org.phoebus.test/value=$(user.home)/documents/file.txt, My File");

        assertTrue(value.contains("my%20user"),
                "Spaces in resolved home path should be encoded: " + value);
        assertFalse(value.contains("My%20File"),
                "Display name should not be encoded: " + value);
    }

    @Test
    public void testBackslashesReplacedOnlyInMacroPath() throws Exception
    {
        // Windows-style path with backslashes
        System.setProperty("phoebus.install", "C:\\Program Files\\Phoebus");

        final String value = loadAndGet(
                "org.phoebus.test/value=$(phoebus.install)/config/guide.pdf?app=web, Style Guide");

        // Backslashes in the path should become forward slashes
        assertTrue(value.contains("C:/Program%20Files/Phoebus"),
                "Backslashes in path should be converted to forward slashes: " + value);
        assertFalse(value.contains("\\"),
                "No backslashes should remain in the resolved path: " + value);
        // Display name should still be fine
        assertFalse(value.contains("Style%20Guide"),
                "Display name should not be encoded: " + value);
    }

    @Test
    public void testNoMacroNoChange() throws Exception
    {
        // Value without any macros should pass through unchanged
        final String value = loadAndGet(
                "org.phoebus.test/value=pv://?&app=probe,PV Probe Tool | https://example.com?app=web, Web App");

        assertThat(value,
                equalTo("pv://?&app=probe,PV Probe Tool | https://example.com?app=web, Web App"));
    }

    @Test
    public void testMultipleMacrosInSameValue() throws Exception
    {
        System.setProperty("phoebus.install", "/opt/my phoebus");
        System.setProperty("user.home", "/home/my user");

        final String value = loadAndGet(
                "org.phoebus.test/value=$(phoebus.install)/a.pdf, Install File | $(user.home)/b.pdf, Home File");

        assertTrue(value.contains("my%20phoebus"),
                "Install path spaces should be encoded: " + value);
        assertTrue(value.contains("my%20user"),
                "Home path spaces should be encoded: " + value);
        assertFalse(value.contains("Install%20File"),
                "Display name should not be encoded: " + value);
        assertFalse(value.contains("Home%20File"),
                "Display name should not be encoded: " + value);
    }
}
