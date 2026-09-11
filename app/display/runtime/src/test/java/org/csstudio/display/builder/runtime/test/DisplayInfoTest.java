/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.junit.jupiter.api.Test;
import org.phoebus.framework.macros.Macros;

import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JUnit test of the {@link DisplayInfo}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayInfoTest
{
    @Test
    public void testDisplayInfo()
    {
        DisplayInfo info = new DisplayInfo("/some/path/file.bob",  null, new Macros(), true);
        assertThat(info.getPath(), equalTo("/some/path/file.bob"));
        assertThat(info.getName(), equalTo("file.bob"));

        info = new DisplayInfo("http://my.site/some/path/file.bob",  null, new Macros(), true);
        assertThat(info.getPath(), equalTo("http://my.site/some/path/file.bob"));
        assertThat(info.getName(), equalTo("file.bob"));

    }

    @Test
    public void testURI2DisplayInfo()
    {
        // Simple example
        URI url = URI.create("file:/some/path/xx.bob");
        DisplayInfo info = DisplayInfo.forURI(url);
        System.out.println(info);

        assertThat(info.getPath(), equalTo("/some/path/xx.bob"));

        // .. with macros
        url = URI.create("file:/some/path/xx.bob?X=Fred+Harvey%20Newman&Y=2");
        info = DisplayInfo.forURI(url);
        System.out.println(info);

        assertThat(info.getPath(), equalTo("/some/path/xx.bob"));
        assertThat(info.getMacros().getValue("X"), equalTo("Fred Harvey Newman"));
        assertThat(info.getMacros().getValue("Y"), equalTo("2"));

        // Web URL
        url = URI.create("http://my.site/some/path/xx.bob?X=Fred+Harvey%20Newman&Y=2");
        info = DisplayInfo.forURI(url);
        System.out.println(info);

        assertThat(info.getPath(), equalTo("http://my.site/some/path/xx.bob"));
        assertThat(info.getMacros().getValue("X"), equalTo("Fred Harvey Newman"));
        assertThat(info.getMacros().getValue("Y"), equalTo("2"));
    }

    @Test
    public void testDisplayInfo2URI()
    {
        // Plain path
        final Macros macros = new Macros();
        DisplayInfo info = new DisplayInfo("/some/path/xx.bob", null, macros, false);
        URI url = info.toURI();
        System.out.println(url);
        assertThat(url.toString(), equalTo("file:/some/path/xx.bob"));

        // .. with macros
        macros.add("X", "Fred Harvey Newman");
        macros.add("Y", "2");
        info = new DisplayInfo("file:/some/path/xx.bob", null, macros, false);
        url = info.toURI();
        System.out.println(url);

        assertThat(url.toString(), equalTo("file:/some/path/xx.bob?X=Fred+Harvey+Newman&Y=2"));

        // Windows
        info = new DisplayInfo("C:\\some\\path\\file.bob",  null, new Macros(), false);
        url = info.toURI();
        System.out.println(url);

        assertThat(url.toString(), equalTo("file:///C:/some/path/file.bob"));
    }

    @Test
    public void testUniqueness() throws Exception
    {
        // URIs using the same macros, but different order
        final URI url1 = new URI("file:/some/path/xx.bob?X=Fred+Harvey%20Newman&Y=2&Z=1");
        final URI url2 = new URI("file:/some/path/xx.bob?Z=1&X=Fred+Harvey%20Newman&Y=2");

        // Should result in equal DisplayInfos
        DisplayInfo info1 = DisplayInfo.forURI(url1);
        DisplayInfo info2 = DisplayInfo.forURI(url2);
        System.out.println(info1);
        System.out.println(info2);
        assertThat(info1, equalTo(info2));
        assertThat(info1, not(sameInstance(info2)));

        // When turned back into a URI, they use the alphabetical ordered macros
        // (like url1, but with '+' for spaces)
        assertThat(info1.toURI().toString(), equalTo(info2.toURI().toString()));
        assertThat(info2.toURI().toString(), equalTo("file:/some/path/xx.bob?X=Fred+Harvey+Newman&Y=2&Z=1"));
    }

    @Test
    public void testForURI_UNC() throws Exception
    {
        // UNC URI with host in authority: file://wsl.localhost/share/path
        final URI unc = new URI("file", "wsl.localhost", "/AlmaLinux-9/home/user/display.bob", null);
        final DisplayInfo info = DisplayInfo.forURI(unc);
        System.out.println("UNC forURI: " + info);

        // Path should have // prefix with host
        assertThat(info.getPath(), equalTo("//wsl.localhost/AlmaLinux-9/home/user/display.bob"));
        assertThat(info.getName(), equalTo("display.bob"));

        // UNC URI with macros
        final URI unc_macros = new URI("file://wsl.localhost/share/path/test.bob?X=1&Y=hello");
        final DisplayInfo info_macros = DisplayInfo.forURI(unc_macros);
        System.out.println("UNC forURI with macros: " + info_macros);

        assertThat(info_macros.getPath(), equalTo("//wsl.localhost/share/path/test.bob"));
        assertThat(info_macros.getMacros().getValue("X"), equalTo("1"));
        assertThat(info_macros.getMacros().getValue("Y"), equalTo("hello"));

        // UNC URI with no host but path starting with // (file:////host/share)
        final URI unc_no_host = URI.create("file:////server/share/path/file.bob");
        final DisplayInfo info_no_host = DisplayInfo.forURI(unc_no_host);
        System.out.println("UNC forURI no host: " + info_no_host);

        // getPath() on such a URI gives "//server/share/path/file.bob"
        assertTrue(info_no_host.getPath().startsWith("//server/"),
                "No-host UNC should preserve //: " + info_no_host.getPath());
    }

    @Test
    public void testToURI_UNC() throws Exception
    {
        // UNC path with // prefix -> toURI should produce file://host/path
        final DisplayInfo info = new DisplayInfo("//wsl.localhost/AlmaLinux-9/home/user/display.bob",
                null, new Macros(), false);
        final URI uri = info.toURI();
        System.out.println("UNC toURI: " + uri);

        // toURI prepends "file:" to path, so the result should be
        // file://wsl.localhost/AlmaLinux-9/home/user/display.bob
        assertThat(uri.toString(), equalTo("file://wsl.localhost/AlmaLinux-9/home/user/display.bob"));
        assertThat(uri.getScheme(), equalTo("file"));
        assertThat(uri.getHost(), equalTo("wsl.localhost"));
        assertThat(uri.getPath(), equalTo("/AlmaLinux-9/home/user/display.bob"));

        // Round-trip: forURI(toURI()) should give back original path
        final DisplayInfo round_trip = DisplayInfo.forURI(uri);
        assertThat(round_trip.getPath(), equalTo("//wsl.localhost/AlmaLinux-9/home/user/display.bob"));

        // UNC path with macros
        final Macros macros = new Macros();
        macros.add("SYS", "BL1");
        final DisplayInfo info_macros = new DisplayInfo("//server/share/displays/main.bob",
                null, macros, false);
        final URI uri_macros = info_macros.toURI();
        System.out.println("UNC toURI with macros: " + uri_macros);

        assertThat(uri_macros.getScheme(), equalTo("file"));
        assertThat(uri_macros.getHost(), equalTo("server"));
        assertTrue(uri_macros.toString().contains("SYS=BL1"),
                "URI should contain macro: " + uri_macros);

        // Round-trip with macros
        final DisplayInfo round_macros = DisplayInfo.forURI(uri_macros);
        assertThat(round_macros.getPath(), equalTo("//server/share/displays/main.bob"));
        assertThat(round_macros.getMacros().getValue("SYS"), equalTo("BL1"));
    }

    @Test
    public void testForModel_UNC() throws Exception
    {
        // Simulate what happens when a model has USER_DATA_INPUT_FILE set to a
        // Windows UNC absolute path (from File.getAbsolutePath() on Windows)
        final DisplayModel model = new DisplayModel();
        model.propMacros().getValue().add("DEVICE", "Motor1");

        // Windows UNC path: \\wsl.localhost\AlmaLinux-9\home\<user>\display.bob
        model.setUserData(DisplayModel.USER_DATA_INPUT_FILE,
                "\\\\wsl.localhost\\AlmaLinux-9\\home\\" + "user\\display.bob");
        DisplayInfo info = DisplayInfo.forModel(model);
        System.out.println("forModel UNC backslash: " + info);

        // Should produce //wsl.localhost/... (not /wsl.localhost/...)
        assertThat(info.getPath(), equalTo("//wsl.localhost/AlmaLinux-9/home/user/display.bob"));
        assertThat(info.getMacros().getValue("DEVICE"), equalTo("Motor1"));

        // Verify full round-trip: forModel -> toURI -> forURI gives same path
        final URI uri = info.toURI();
        System.out.println("forModel -> toURI: " + uri);
        assertThat(uri.getHost(), equalTo("wsl.localhost"));

        final DisplayInfo round_trip = DisplayInfo.forURI(uri);
        assertThat(round_trip.getPath(), equalTo("//wsl.localhost/AlmaLinux-9/home/user/display.bob"));

        // Unix-style UNC path (already forward slashes)
        model.setUserData(DisplayModel.USER_DATA_INPUT_FILE,
                "//wsl.localhost/AlmaLinux-9/home/user/display.bob");
        info = DisplayInfo.forModel(model);
        System.out.println("forModel UNC forward slash: " + info);

        // Starts with "/" so goes through else branch, should be unchanged
        assertThat(info.getPath(), equalTo("//wsl.localhost/AlmaLinux-9/home/user/display.bob"));

        // Regular Windows path (not UNC) should still get "/" prepended
        model.setUserData(DisplayModel.USER_DATA_INPUT_FILE,
                "C:\\Users\\test\\display.bob");
        info = DisplayInfo.forModel(model);
        System.out.println("forModel Windows: " + info);
        assertThat(info.getPath(), equalTo("/C:/Users/test/display.bob"));

        // Regular Unix path should pass through unchanged
        model.setUserData(DisplayModel.USER_DATA_INPUT_FILE,
                "/home/user/displays/main.bob");
        info = DisplayInfo.forModel(model);
        System.out.println("forModel Unix: " + info);
        assertThat(info.getPath(), equalTo("/home/user/displays/main.bob"));
    }
}
