/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.display.builder.runtime.TextPatch;
import org.junit.Test;

/** JUnit demo of the {@link TextPatch}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TextPatchTest
{
    @Test
    public void testLongString() throws Exception
    {
        final TextPatch pvm_string = new TextPatch(" \\{\"longString\":true\\}", "");

        String name = "fred {\"longString\":true}";
        String patched = pvm_string.patch(name);
        System.out.println(name + " -> " + patched);
        assertThat(patched, equalTo("fred"));
    }

    @Test
    public void testConstantNumber() throws Exception
    {
        final TextPatch pvm_string = new TextPatch("^=([0-9]+)", "loc://const$1($1)");

        String name = "=1";
        String patched = pvm_string.patch(name);
        System.out.println(name + " -> " + patched);
        assertThat(patched, equalTo("loc://const1(1)"));

        name = "=42";
        patched = pvm_string.patch(name);
        System.out.println(name + " -> " + patched);
        assertThat(patched, equalTo("loc://const42(42)"));

        // Can a PV name really contain '='?!
        // Evidently, yes..
        name = "test=0";
        patched = pvm_string.patch(name);
        System.out.println(name + " -> " + patched);
        assertThat(patched, equalTo("test=0"));
    }

    @Test
    public void testConstantText() throws Exception
    {
        final TextPatch pvm_string = new TextPatch("^=\"([a-zA-Z]+)\"", "loc://str$1(\"$1\")");

        String name = "=\"Fred\"";
        String patched = pvm_string.patch(name);
        System.out.println(name + " -> " + patched);
        assertThat(patched, equalTo("loc://strFred(\"Fred\")"));

        // PV with '=' in the name?!
        name = "test=egon";
        patched = pvm_string.patch(name);
        System.out.println(name + " -> " + patched);
        assertThat(patched, equalTo("test=egon"));
    }
}
