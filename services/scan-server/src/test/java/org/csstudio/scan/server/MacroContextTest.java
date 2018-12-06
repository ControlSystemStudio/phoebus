/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

@SuppressWarnings("nls")
public class MacroContextTest
{
    @Test
    public void testMacroContext() throws Exception
    {
        final MacroContext context = new MacroContext("A=a1, B=b1");
        assertThat(context.getValue("A"), equalTo("a1"));
        assertThat(context.getValue("B"), equalTo("b1"));

        context.pushMacros("A=a2");
        assertThat(context.getValue("A"), equalTo("a2"));
        assertThat(context.getValue("B"), equalTo("b1"));

        context.pushMacros("B=b2");
        assertThat(context.getValue("A"), equalTo("a2"));
        assertThat(context.getValue("B"), equalTo("b2"));

        context.popMacros();
        assertThat(context.getValue("A"), equalTo("a2"));
        assertThat(context.getValue("B"), equalTo("b1"));

        context.popMacros();
        assertThat(context.getValue("A"), equalTo("a1"));
        assertThat(context.getValue("B"), equalTo("b1"));
    }
}
