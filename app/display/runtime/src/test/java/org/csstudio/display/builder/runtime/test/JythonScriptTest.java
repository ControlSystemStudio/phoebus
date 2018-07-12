/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
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
import org.csstudio.display.builder.runtime.script.internal.Script;
import org.csstudio.display.builder.runtime.script.internal.ScriptSupport;
import org.junit.Test;

/** JUnit test of script support
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JythonScriptTest
{
    @Test
    public void testJythonScript() throws Exception
    {
        System.setProperty("python.import.site", "false");

        // Load some display with a label
        final DisplayModel display = ModelLoader.loadModel(getClass().getResourceAsStream("/rt_examples/script_test.opi"), "script_test.opi");
        final Widget widget = display.runtimeChildren().getChildByName("Label 100");

        // Set widget variable in script
        final ScriptSupport scripting = new ScriptSupport();
        final Script script = scripting.compile(".",
                                                "updateText.py",
                                                getClass().getResourceAsStream("/rt_examples/updateText.py"));
        // Run the script several times
        for (int run=0; run<10; ++run)
        {   // Script should set the "text" to "Hello"
            widget.setPropertyValue("text", "Initial");
            assertThat(widget.getPropertyValue("text"), equalTo("Initial"));
            script.submit(widget).get();
            assertThat(widget.getPropertyValue("text"), equalTo("Hello"));
        }

        scripting.close();
    }
}
