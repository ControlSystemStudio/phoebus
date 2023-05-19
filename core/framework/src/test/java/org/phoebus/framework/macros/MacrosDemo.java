/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.macros;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

/** JUnit test of macro handling
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacrosDemo
{
    @Test
    public void testMacros() throws Exception
    {
        // Macro specs provided by the launcher
        final Macros2 launcher = new Macros2();
        launcher.add("L", "launcher");

        // Macro specs provided by the display
        final Macros2 display = new Macros2();
        display.add("T", "display");

        // Macro specs provided by a group inside the display
        final Macros2 group = new Macros2();
        group.add("T", "group");

        // Hierarchically expand specs
        System.out.println(group);
        launcher.expand(null);
        display.expand(launcher);
        group.expand(display);
        System.out.println(group);

        // MacroValues contain the expanded values
        assertThat(display.getValue("T"), equalTo("display"));
        assertThat(group.getValue("T"), equalTo("group"));
    }
}
