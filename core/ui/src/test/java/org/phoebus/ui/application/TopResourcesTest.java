/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.application;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.junit.Test;

/** Demo of the TopResources API
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TopResourcesTest
{
    @Test
    public void testTopResourcesParsing() throws Exception
    {
        TopResources tops = TopResources.parse("");
        assertThat(tops.size(), equalTo(0));

        tops = TopResources.parse("uri1|uri2");
        assertThat(tops.size(), equalTo(2));
        assertThat(tops.getResource(1), equalTo(URI.create("uri2")));

        tops = TopResources.parse("examples:/01_main.bob,Example Display|pv://?sim://sine&app=probe,Probe Example");
        assertThat(tops.size(), equalTo(2));
        assertThat(tops.getResource(1), equalTo(URI.create("pv://?sim://sine&app=probe")));
        assertThat(tops.getDescription(1), equalTo("Probe Example"));
    }
}
