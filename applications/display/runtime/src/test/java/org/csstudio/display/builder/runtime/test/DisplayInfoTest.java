/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.junit.Test;
import org.phoebus.framework.macros.Macros;

/** JUnit test of the {@link DisplayInfo}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayInfoTest
{
    @Test
    public void testDisplayInfo() throws Exception
    {
        DisplayInfo info = new DisplayInfo("/some/path/file.bob",  null, new Macros(), true);
        assertThat(info.getPath(), equalTo("/some/path/file.bob"));
        assertThat(info.getName(), equalTo("file.bob"));

        info = new DisplayInfo("http://my.site/some/path/file.bob",  null, new Macros(), true);
        assertThat(info.getPath(), equalTo("http://my.site/some/path/file.bob"));
        assertThat(info.getName(), equalTo("file.bob"));

    }

    @Test
    public void testURI2DisplayInfo() throws Exception
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
    public void testDisplayInfo2URI() throws Exception
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
}
