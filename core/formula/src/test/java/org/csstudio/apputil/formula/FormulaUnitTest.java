/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/** Formula tests.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaUnitTest
{
    private final static double epsilon = 0.001;

    @Test
    public void testBasics() throws Exception
    {
        Formula f = new Formula("0");
        assertEquals("0.0", f.toString());
        assertEquals(0.0, f.eval(), epsilon);

        // floating point
        f = new Formula("-3.14");
        assertEquals(-3.14, f.eval(), epsilon);

        // exponential
        f = new Formula("-2.123e4");
        assertEquals(-2.123e4, f.eval(), epsilon);

        // exponential
        f = new Formula("-2.123e-14");
        assertEquals(-2.123e-14, f.eval(), epsilon);

        f = new Formula("-3.14 + 2");
        assertEquals("(-3.14 + 2.0)", f.toString());
        assertEquals(-1.14, f.eval(), epsilon);

        f = new Formula("-3.14 - 2");
        assertEquals(-5.14, f.eval(), epsilon);

        f = new Formula("-3.14 + 2 - 1.10");
        assertEquals(-2.24, f.eval(), epsilon);

        f = new Formula("-12/-3");
        assertEquals(4.0, f.eval(), epsilon);

        // Order, quotes
        f = new Formula("1 + 2 * 3 - 4");
        assertEquals(3.0, f.eval(), epsilon);

        f = new Formula("(1 + 2) * (3 - 4)");
        assertEquals(-3.0, f.eval(), epsilon);

        // quotes
        f = new Formula("-(3.14)");
        assertEquals(-3.14, f.eval(), epsilon);

        f = new Formula("-(-3.14)");
        assertEquals(+3.14, f.eval(), epsilon);
    }

    @Test
    public void testBool() throws Exception
    {
        Formula f = new Formula("2 & 3");
        assertEquals(1.0, f.eval(), epsilon);

        f = new Formula("2 == 3");
        assertEquals(0.0, f.eval(), epsilon);

        f = new Formula("2 != 3");
        assertEquals(1.0, f.eval(), epsilon);

        f = new Formula("!0");
        assertEquals(1.0, f.eval(), epsilon);

        f = new Formula("2 & 0");
        assertEquals(0.0, f.eval(), epsilon);

        f = new Formula("0 | 3");
        assertEquals(1.0, f.eval(), epsilon);

        f = new Formula("0 | 0");
        assertEquals(0.0, f.eval(), epsilon);
    }

    @Test
    public void testFunctions() throws Exception
    {
        Formula f = new Formula("sqrt(2) ^ 2");
        assertEquals(2.0, f.eval(), epsilon);

        f = new Formula("exp(log(2))");
        assertEquals(2.0, f.eval(), epsilon);

        f = new Formula("2 ? 3 : 4");
        assertEquals(3.0, f.eval(), epsilon);

        f = new Formula("0 ? 3 : 4");
        assertEquals(4.0, f.eval(), epsilon);

        // Sequence of  x ? 1 : x ? 2 : 3
        // Get 1, 2, 3:
        f = new Formula("10<20 ? 1 : 10>20 ? 2 : 3");
        assertEquals(1.0, f.eval(), epsilon);

        f = new Formula("10>20 ? 1 : 10<20 ? 2 : 3");
        assertEquals(2.0, f.eval(), epsilon);

        f = new Formula("10>20 ? 1 : 10>20 ? 2 : 3");
        assertEquals(3.0, f.eval(), epsilon);

        f = new Formula("2>1 ? 3 : 4");
        assertEquals(3.0, f.eval(), epsilon);

        f = new Formula("!(2>1 ? 0 : 1)");
        assertEquals(1.0, f.eval(), epsilon);

        f = new Formula("(2<1) ? 3 : 4");
        assertEquals(4.0, f.eval(), epsilon);

        f = new Formula("(2<=2) ? 3 : 4");
        assertEquals(3.0, f.eval(), epsilon);

        f = new Formula("sqrt(2)");
        assertEquals(1.414, f.eval(), epsilon);

        f = new Formula("min(5, 4, 3, 2, 1)");
        assertEquals(1, f.eval(), epsilon);

        f = new Formula("max(5, 4, 3, 2, 1)");
        assertEquals(5, f.eval(), epsilon);

        f = new Formula("sin(" + Math.toRadians(30) + ")");
        assertEquals(0.5, f.eval(), epsilon);

        f = new Formula("sin(toRadians(30))");
        assertEquals(0.5, f.eval(), epsilon);

        f = new Formula("cos(30)");
        assertEquals(0.1543, f.eval(), epsilon);

        f = new Formula("atan2(10.0, 0.0)");
        assertEquals(90.0, Math.toDegrees(f.eval()), epsilon);

        f = new Formula("rnd(10.0)");
        for (int i=0; i<50; ++i)
        {
            double rnd = f.eval();
            assertTrue(rnd >= 0.0);
            assertTrue(rnd < 10.0);
            double rnd2 = f.eval();
            // usually, should NOT get the same number twice...
            assertTrue(rnd != rnd2);
        }
    }

    @Test
    public void testVariables() throws Exception
    {
        VariableNode v[] = new VariableNode[2];
        v[0] = new VariableNode("volt");
        v[1] = new VariableNode("curr");
        v[0].setValue(2.0);
        v[1].setValue(3.0);

        Formula f = new Formula("0.5 * volt * curr", v);
        assertEquals(3.0, f.eval(), epsilon);

        v[0].setValue(20.0);
        v[1].setValue(30.0);
        assertEquals(300.0, f.eval(), epsilon);

        v[0].setValue(2.0);
        v[1].setValue(3.0);
        assertEquals(3.0, f.eval(), epsilon);

        f = new Formula("max(volt, curr, -2)", v);
        assertEquals(3.0, f.eval(), epsilon);

        f = new Formula("2*PI", v);
        assertEquals(2.0*Math.PI, f.eval(), epsilon);

        v[0] = new VariableNode("PI", 10.0);
        f = new Formula("PI", v);
        assertEquals(10.0, f.eval(), epsilon);
        assertTrue(f.hasSubnode(v[0]));
        assertTrue(! f.hasSubnode(v[1]));

        assertTrue(f.hasSubnode(v[0].getName()));
        assertTrue(! f.hasSubnode(v[1].getName()));

    }

    @Test
    public void testErrors() throws Exception
    {
        Formula f;
        try
        {
            f = new Formula("-");
            fail("Didn't catch parse error");
        }
        catch (Exception ex)
        {
            assertEquals("Unexpected end of formula.", ex.getMessage());
        }

        try
        {
            f = new Formula("max 2");
            fail("Didn't catch parse error");
        }
        catch (Exception ex)
        {
            // Can the scanner be fixed to get 'max' instead of 'max2'
            // for the var. name?
            assertEquals("Unknown variable 'max2'", ex.getMessage());
        }

        // Not a formula error, but gives Infinity resp. NaN
        f = new Formula("1/0");
        assertTrue(Double.isInfinite(f.eval()));

        f = new Formula("sqrt(-1)");
        assertTrue(Double.isNaN(f.eval()));
    }


    @Test
    public void testVariableDetermination() throws Exception
    {
        Formula f = new Formula("RFQ_Vac:Pump2:Pressure < 10", true);
        VariableNode vars[] = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("RFQ_Vac:Pump2:Pressure", vars[0].getName());
        vars[0].setValue(5);
        assertEquals(1.0, f.eval(), epsilon);
        vars[0].setValue(10);
        assertEquals(0.0, f.eval(), epsilon);

        // PV with special characters
        f = new Formula("'IOC2049-102:BMIT:enabled' >= 10", true);
        vars = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("IOC2049-102:BMIT:enabled", vars[0].getName());
        vars[0].setValue(5);
        assertEquals(0.0, f.eval(), epsilon);
        vars[0].setValue(10);
        assertEquals(1.0, f.eval(), epsilon);

        assertTrue(f.hasSubnode("IOC2049-102:BMIT:enabled"));
        assertFalse(f.hasSubnode("Fred"));

        // ITER example PV that starts with number
        f = new Formula("'51RS1-COS-1:OPSTATE' != 3", true);
        vars = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("51RS1-COS-1:OPSTATE", vars[0].getName());
        vars[0].setValue(3);
        assertEquals(0.0, f.eval(), epsilon);
        vars[0].setValue(4);
        assertEquals(1.0, f.eval(), epsilon);

        // Allow backticks for PV name
        f = new Formula("`SomePV` + 3", true);
        vars = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("SomePV", vars[0].getName());
        vars[0].setValue(1);
        assertEquals(4.0, f.eval(), epsilon);
    }
}
