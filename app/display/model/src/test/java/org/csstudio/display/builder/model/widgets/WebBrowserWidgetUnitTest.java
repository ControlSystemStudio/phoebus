/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.junit.jupiter.api.Test;

/** JUnit tests for {@link WebBrowserWidget}'s 'resize_with_window' property.
 *
 *  <p>Verifies the default value and XML round-trip persistence for the
 *  backward-compatible new property.
 *
 *  @author Gianluca Martino
 */
@SuppressWarnings("nls")
public class WebBrowserWidgetUnitTest
{
    /** 'resize_with_window' must default to false so existing displays are unaffected */
    @Test
    public void testResizeWithWindowDefault()
    {
        final WebBrowserWidget browser = new WebBrowserWidget();
        assertThat(browser.propResizeWithWindow().getValue(), equalTo(false));
    }

    /** A non-default 'resize_with_window' must survive an XML round trip */
    @Test
    public void testResizeWithWindowRoundTrip() throws Exception
    {
        final WebBrowserWidget original = new WebBrowserWidget();
        original.propResizeWithWindow().setValue(true);

        final DisplayModel model = new DisplayModel();
        model.runtimeChildren().addChild(original);
        // The value is non-default (true), so it is written regardless of skip_defaults.
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ModelWriter writer = new ModelWriter(out);
        writer.writeModel(model);
        writer.close();
        final String xml = out.toString();
        assertThat(xml, containsString("<resize_with_window>"));

        final ModelReader reader = new ModelReader(new ByteArrayInputStream(xml.getBytes()));
        final DisplayModel loaded = reader.readModel();
        final Widget w = loaded.getChildren().get(0);
        assertTrue(w instanceof WebBrowserWidget);
        assertThat(((WebBrowserWidget) w).propResizeWithWindow().getValue(), equalTo(true));
    }

    /** A default 'resize_with_window' must be omitted from XML (older phoebus ignores it anyway) */
    @Test
    public void testResizeWithWindowOmittedWhenDefault() throws Exception
    {
        final WebBrowserWidget browser = new WebBrowserWidget();
        final DisplayModel model = new DisplayModel();
        model.runtimeChildren().addChild(browser);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ModelWriter writer = new ModelWriter(out);
        writer.writeModel(model);
        writer.close();
        assertThat(out.toString(), not(containsString("<resize_with_window>")));
    }
}
