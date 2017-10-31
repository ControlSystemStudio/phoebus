/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import static org.hamcrest.CoreMatchers.equalTo;
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
        DisplayInfo info = new DisplayInfo("file:/some/path/xx.bob", null, macros, false);
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
}
