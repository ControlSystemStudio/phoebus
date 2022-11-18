/*******************************************************************************
 * Copyright (c) 2015-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.rules;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/** Test replacement of logical operators in RuleToScript.
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class RuleToScript_ReplacementTests
{
    @Test
    public void testTrueFalse()
    {
        assertThat(RuleToScript.javascriptToPythonLogic(" true "), equalTo(" True "));
        assertThat(RuleToScript.javascriptToPythonLogic(" ! false "), equalTo("  not  False "));
        assertThat(RuleToScript.javascriptToPythonLogic("  \"false\"  "), equalTo("  \"false\"  "));
    }

    @Test
    public void testEqual()
    {
        assertThat(RuleToScript.javascriptToPythonLogic("pv0!=3"), equalTo("pv0!=3"));
        assertThat(RuleToScript.javascriptToPythonLogic("pv0=3"), equalTo("pv0==3"));
        assertThat(RuleToScript.javascriptToPythonLogic("pv0>=0"), equalTo("pv0>=0"));
    }

    @Test
    public void testAndOr()
    {
        assertThat(RuleToScript.javascriptToPythonLogic("x && y"), equalTo("x  and  y"));
        assertThat(RuleToScript.javascriptToPythonLogic("x || y"), equalTo("x  or  y"));

        assertThat(RuleToScript.javascriptToPythonLogic("a < 1 && y != \"a && b\""), equalTo("a < 1  and  y != \"a && b\""));
        assertThat(RuleToScript.javascriptToPythonLogic("y != \"a && b\" && a < 1"), equalTo("y != \"a && b\"  and  a < 1"));

        assertThat(RuleToScript.javascriptToPythonLogic("a < 1 || y != \"a || b\""), equalTo("a < 1  or  y != \"a || b\""));
    }

    @Test
    public void testBitwise()
    {
        // Bitwise operations are left untouched, but will only "work" when using pvInt,
        // because Python/Jython won't handle them with float numbers.
        // Javascript silently casts to integer, so some *.opi will need their rules to be updated
        // from "pv.." to "pvInt.." to work in both tools
        assertThat(RuleToScript.javascriptToPythonLogic("pvInt2 & 4"), equalTo("pvInt2 & 4"));
        assertThat(RuleToScript.javascriptToPythonLogic("pvInt2 | 4"), equalTo("pvInt2 | 4"));
        assertThat(RuleToScript.javascriptToPythonLogic("pvInt2 ^ 4"), equalTo("pvInt2 ^ 4"));
    }

    @Test
    public void testNot()
    {
        assertThat(RuleToScript.javascriptToPythonLogic("x != y"), equalTo("x != y"));
        assertThat(RuleToScript.javascriptToPythonLogic("!(x+y > 1)"), equalTo(" not (x+y > 1)"));
        assertThat(RuleToScript.javascriptToPythonLogic("!(a==b) && x != \"!\""), equalTo(" not (a==b)  and  x != \"!\""));

        // In this nonsense input, the '!' is replaced by ' not '
        assertThat(RuleToScript.javascriptToPythonLogic("Hello World!"), equalTo("Hello World not "));

        // Inside quotes, the '!' is preserved
        assertThat(RuleToScript.javascriptToPythonLogic("\"Hello World !\""), equalTo("\"Hello World !\""));

        // Same for single quotes
        assertThat(RuleToScript.javascriptToPythonLogic("'Hello World !'"), equalTo("'Hello World !'"));
    }

    @Test
    public void testAll()
    {
        //                         a == "a"   || x != "\""     && y == "b!"
        final String expression = "a == \"a\" || x != \"\\\"\" && y == \"b!\"";
        assertThat(RuleToScript.javascriptToPythonLogic(expression),
                                                equalTo("a == \"a\"  or  x != \"\\\"\"  and  y == \"b!\""));
        assertThat(RuleToScript.javascriptToPythonLogic("pv0>=0) && (pv0<=10)"), equalTo("pv0>=0)  and  (pv0<=10)"));
    }
}
