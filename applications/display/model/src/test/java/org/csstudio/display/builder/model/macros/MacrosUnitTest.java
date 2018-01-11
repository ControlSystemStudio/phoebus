/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.phoebus.framework.macros.Macros;

/** JUnit test of macro handling
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacrosUnitTest
{
    @Test
    public void testXML() throws Exception
    {
        final Macros macros = new Macros();
        macros.add("S", "System");
        macros.add("N", "42");

        final String xml = MacroXMLUtil.toString(macros);
        System.out.println(xml);
        assertThat(xml, equalTo("<N>42</N><S>System</S>"));

        final Macros readback = MacroXMLUtil.readMacros(xml);
        assertThat(readback.getValue("S"), equalTo("System"));
        assertThat(readback.getNames(), hasItems("S", "N"));
    }
}
