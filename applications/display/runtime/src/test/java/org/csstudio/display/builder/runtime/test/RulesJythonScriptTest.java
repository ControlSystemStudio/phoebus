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

import java.io.ByteArrayInputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.rules.RuleToScript;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.script.internal.Script;
import org.csstudio.display.builder.runtime.script.internal.ScriptSupport;
import org.junit.Test;

/** JUnit test of rule support
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RulesJythonScriptTest
{
    /** Test handling of rules step-by-step */
    @Test
    public void testRule() throws Exception
    {
        // Load a display which contains rectangle with rule-based width
        final DisplayModel display = ModelLoader.loadModel(getClass().getResourceAsStream("/rt_examples/rule_demo.opi"), "rule_demo.opi");
        final Widget widget = display.runtimeChildren().getChildByName("Rectangle");
        System.out.println(widget);
        final RuleInfo rule = widget.propRules().getValue().get(0);
        System.out.println(rule);
        assertThat(rule.getPropID(), equalTo("width"));

        // Convert rule into script
        // (add printout to show when the script executes)
        final String script_text = "print 'Executing the script!'\n" + RuleToScript.generatePy(widget, rule);
        System.out.println(script_text);

        // Runtime would create the PVs used by the rule...
        final RuntimePV pv = PVFactory.getPV(rule.getPVs().get(0).getName());

        // Runtime would compile the script...
        final ScriptSupport scripting = new ScriptSupport();
        final Script rule_script = scripting.compile(".", "rule.py", new ByteArrayInputStream(script_text.getBytes()));

        // .. and then execute it whenever the PV changes.
        // Scripts are executed in background.
        // Check for updates of the affected property
        // to detect when the script executed.
        final Semaphore width_changed = new Semaphore(0);
        widget.propWidth().addPropertyListener((prop, old, width) ->
        {
            System.out.println(pv + " = " + pv.read() + "  ->  " + widget.propWidth());
            width_changed.release();
        });

        // Widget width starts out at 200
        assertThat(widget.propWidth().getValue(), equalTo(200));

        // For PV == 1, rule should change it to 100
        pv.write(1);
        rule_script.submit(widget, pv);
        // Wait for script to execute..
        assertThat(width_changed.tryAcquire(5, TimeUnit.SECONDS), equalTo(true));
        assertThat(widget.propWidth().getValue(), equalTo(100));

        // For PV == 0, rule should change it back to 200
        pv.write(0);
        rule_script.submit(widget, pv);
        // Wait for script to execute..
        assertThat(width_changed.tryAcquire(5, TimeUnit.SECONDS), equalTo(true));
        assertThat(widget.propWidth().getValue(), equalTo(200));

        // Cleanup
        scripting.close();
        PVFactory.releasePV(pv);
    }
}
