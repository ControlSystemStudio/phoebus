/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.runtime.script.internal.PythonGatewaySupport;
import org.csstudio.display.builder.runtime.script.internal.Script;
import org.csstudio.display.builder.runtime.script.internal.ScriptSupport;
import org.junit.Test;

/** JUnit test of script support
 *  Based on {@link JythonScriptTest}
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class PythonScriptTest
{
    @Test
    public void testPythonScript() throws Exception
    {
        if (! PythonGatewaySupport.isConnect2jInstalled())
        {
            System.err.println("Skipping PythonScriptTest because there is no python with connect2j");
            return;
        }

        final DisplayModel display = ModelLoader.resolveAndLoadModel("../org.csstudio.display.builder.runtime.test/examples/dummy.opi", "script_test.opi");

        final Widget widget = display.getChildren().parallelStream().filter(w -> w.getName().equals("Label 100")).findFirst().get();

        System.out.println(widget);

        // Set widget variable in script
        final ScriptSupport scripting = new ScriptSupport();
        final Script script = scripting.compile("../org.csstudio.display.builder.runtime.test/examples", "updateText_python.py", null);
        for (int run = 0; run < 10; ++run)
        {
            widget.setPropertyValue("text", "Initial");
            assertThat(widget.getPropertyValue("text"), equalTo("Initial"));
            script.submit(widget).get();
            assertThat(widget.getPropertyValue("text"), equalTo("Hello"));
        }

        scripting.close();
    }
}
