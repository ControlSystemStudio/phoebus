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

import java.net.URL;

import org.csstudio.display.builder.model.macros.Macros;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.junit.Test;

/** JUnit test of the {@link DisplayInfo}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayInfoTest
{
    @Test
    public void testDisplayInfo() throws Exception
    {
        // 'Name' defaults to basename of path
        DisplayInfo info = new DisplayInfo("/some/path/file.bob", null, new Macros(), true);
        assertThat(info.getPath(), equalTo("/some/path/file.bob"));
        assertThat(info.getName(), equalTo("file.bob"));

        // Name provided
        info = new DisplayInfo("/some/path/file.bob", "My Display", new Macros(), true);
        assertThat(info.getPath(), equalTo("/some/path/file.bob"));
        assertThat(info.getName(), equalTo("My Display"));
    }

    @Test
    public void testURL2DisplayInfo() throws Exception
    {
        // Simple example
        URL url = new URL("file:/some/path/xx.bob");
        DisplayInfo info = DisplayInfo.forURL(url);
        System.out.println(info);

        assertThat(info.getPath(), equalTo("/some/path/xx.bob"));
        assertThat(info.getName(), equalTo("xx.bob"));

        // Complete example with display name and macros
        url = new URL("file:/some/path/xx.bob;X=Fred+Harvey%20Newman;Y=2?query=ignored#X%20Overview");
        info = DisplayInfo.forURL(url);
        System.out.println(info);

        assertThat(info.getPath(), equalTo("/some/path/xx.bob"));
        assertThat(info.getName(), equalTo("X Overview"));
        assertThat(info.getMacros().getValue("X"), equalTo("Fred Harvey Newman"));
        assertThat(info.getMacros().getValue("Y"), equalTo("2"));
    }

    @Test
    public void testDisplayInfo2URL() throws Exception
    {
        // Plain path
        final Macros macros = new Macros();
        DisplayInfo info = new DisplayInfo("/some/path/xx.bob", null, macros, false);
        URL url = info.toURL();
        System.out.println(url);
        assertThat(url.toString(), equalTo("file:/some/path/xx.bob"));

        // .. with macros
        macros.add("X", "Fred Harvey Newman");
        macros.add("Y", "2");
        info = new DisplayInfo("file:/some/path/xx.bob", null, macros, false);
        url = info.toURL();
        System.out.println(url);

        assertThat(url.toString(), equalTo("file:/some/path/xx.bob;X=Fred+Harvey+Newman;Y=2"));

        // .. and display name
        info = new DisplayInfo("file:/some/path/xx.bob", "X Overview", macros, false);
        url = info.toURL();
        System.out.println(url);

        assertThat(url.toString(), equalTo("file:/some/path/xx.bob;X=Fred+Harvey+Newman;Y=2#X+Overview"));
    }

    @Test
    public void testUniqueness() throws Exception
    {
        // URLs using the same macros, but different order
        final URL url1 = new URL("file:/some/path/xx.bob;X=Fred+Harvey%20Newman;Y=2;Z=1");
        final URL url2 = new URL("file:/some/path/xx.bob;Z=1;X=Fred+Harvey%20Newman;Y=2");

        // Should result in equal DisplayInfos
        DisplayInfo info1 = DisplayInfo.forURL(url1);
        DisplayInfo info2 = DisplayInfo.forURL(url2);
        System.out.println(info1);
        System.out.println(info2);
        assertThat(info1, equalTo(info2));
        assertThat(info1, not(sameInstance(info2)));

        // When turned back into a URL, they use the alphabetical ordered macros
        // (like url1, but with '+' for spaces)
        assertThat(info1.toURL().toString(), equalTo(info2.toURL().toString()));
        assertThat(info2.toURL().toString(), equalTo("file:/some/path/xx.bob;X=Fred+Harvey+Newman;Y=2;Z=1"));
    }
}
