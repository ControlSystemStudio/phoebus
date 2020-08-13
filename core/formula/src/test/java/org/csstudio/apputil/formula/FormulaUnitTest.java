/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;


import org.phoebus.core.vtypes.VTypeHelper;

import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.VString;
import org.epics.vtype.Time;


/** Formula tests.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaUnitTest
{
    private final static double epsilon = 0.001;

    @BeforeClass
    public static void setup()
    {
        final Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (Handler handler : root.getHandlers())
            handler.setLevel(root.getLevel());
    }

    @Test
    public void testBasics() throws Exception
    {
        Formula f = new Formula("0");
        assertEquals("0.0", f.toString());
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);

        // floating point
        f = new Formula("-3.14");
        assertEquals(-3.14, VTypeHelper.toDouble(f.eval()), epsilon);

        // exponential
        f = new Formula("-2.123e4");
        assertEquals(-2.123e4, VTypeHelper.toDouble(f.eval()), epsilon);

        // exponential
        f = new Formula("-2.123e-14");
        assertEquals(-2.123e-14, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("-3.14 + 2");
        assertEquals("(-3.14 + 2.0)", f.toString());
        assertEquals(-1.14, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("-3.14 - 2");
        assertEquals(-5.14, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("-3.14 + 2 - 1.10");
        assertEquals(-2.24, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("-12/-3");
        assertEquals(4.0, VTypeHelper.toDouble(f.eval()), epsilon);

        // Order, quotes
        f = new Formula("1 + 2 * 3 - 4");
        assertEquals(3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("(1 + 2) * (3 - 4)");
        assertEquals(-3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        // quotes
        f = new Formula("-(3.14)");
        assertEquals(-3.14, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("-(-3.14)");
        assertEquals(+3.14, VTypeHelper.toDouble(f.eval()), epsilon);
    }

    @Test
    public void testBool() throws Exception
    {
        Formula f = new Formula("2 & 3");
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("2 == 3");
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("2 != 3");
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("!0");
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("2 & 0");
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("2 & !0");
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("!(2 & !0)");
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("0 | 3");
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("0 | 0");
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);
    }

    @Test
    public void testFunctions() throws Exception
    {
        Formula f = new Formula("sqrt(2) ^ 2");
        assertEquals(2.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("exp(log(2))");
        assertEquals(2.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("2 ? 3 : 4");
        assertEquals(3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("0 ? 3 : 4");
        assertEquals(4.0, VTypeHelper.toDouble(f.eval()), epsilon);

        // Sequence of  x ? 1 : x ? 2 : 3
        // Get 1, 2, 3:
        f = new Formula("10<20 ? 1 : 10>20 ? 2 : 3");
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("10>20 ? 1 : 10<20 ? 2 : 3");
        assertEquals(2.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("10>20 ? 1 : 10>20 ? 2 : 3");
        assertEquals(3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("2>1 ? 3 : 4");
        assertEquals(3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("!(2>1 ? 0 : 1)");
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("(2<1) ? 3 : 4");
        assertEquals(4.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("(2<=2) ? 3 : 4");
        assertEquals(3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("sqrt(2)");
        assertEquals(1.414, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("min(5, 4, 3, 2, 1)");
        assertEquals(1, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("max(5, 4, 3, 2, 1)");
        assertEquals(5, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("sin(" + Math.toRadians(30) + ")");
        assertEquals(0.5, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("sin(toRadians(30))");
        assertEquals(0.5, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("sin(PI/2)");
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("cos(30)");
        assertEquals(0.1543, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("atan2(10.0, 0.0)");
        assertEquals(90.0, Math.toDegrees(VTypeHelper.toDouble(f.eval())), epsilon);

        f = new Formula("pow(10.0, 3.0)");
        assertEquals(1000.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("rnd(10.0)");
        for (int i=0; i<50; ++i)
        {
            double rnd = VTypeHelper.toDouble(f.eval());
            assertTrue(rnd >= 0.0);
            assertTrue(rnd < 10.0);
            double rnd2 = VTypeHelper.toDouble(f.eval());
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
        assertEquals(3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        v[0].setValue(20.0);
        v[1].setValue(30.0);
        assertEquals(300.0, VTypeHelper.toDouble(f.eval()), epsilon);

        v[0].setValue(2.0);
        v[1].setValue(3.0);
        assertEquals(3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("max(volt, curr, -2)", v);
        assertEquals(3.0, VTypeHelper.toDouble(f.eval()), epsilon);

        f = new Formula("2*PI", v);
        assertEquals(2.0*Math.PI, VTypeHelper.toDouble(f.eval()), epsilon);

        v[0] = new VariableNode("PI", 10.0);
        f = new Formula("PI", v);
        assertEquals(10.0, VTypeHelper.toDouble(f.eval()), epsilon);
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
        assertTrue(Double.isInfinite(VTypeHelper.toDouble(f.eval())));

        f = new Formula("sqrt(-1)");
        assertTrue(Double.isNaN(VTypeHelper.toDouble(f.eval())));
    }

    @Test
    public void testSPI() throws Exception
    {
        Formula f = new Formula("fac(3)");
        assertEquals(6.0, VTypeHelper.toDouble(f.eval()), epsilon);

        try
        {
            f = new Formula("fac(2, 3)");
            fail("Didn't catch argument mismatch");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), containsString("arguments"));
        }
    }

    @Test
    public void testVariableDetermination() throws Exception
    {
        Formula f = new Formula("RFQ_Vac:Pump2:Pressure < 10", true);
        VariableNode vars[] = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("RFQ_Vac:Pump2:Pressure", vars[0].getName());
        vars[0].setValue(5);
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);
        vars[0].setValue(10);
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);

        // PV with special characters
        f = new Formula("'IOC2049-102:BMIT:enabled' >= 10", true);
        vars = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("IOC2049-102:BMIT:enabled", vars[0].getName());
        vars[0].setValue(5);
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);
        vars[0].setValue(10);
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        assertTrue(f.hasSubnode("IOC2049-102:BMIT:enabled"));
        assertFalse(f.hasSubnode("Fred"));

        // ITER example PV that starts with number
        f = new Formula("'51RS1-COS-1:OPSTATE' != 3", true);
        vars = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("51RS1-COS-1:OPSTATE", vars[0].getName());
        vars[0].setValue(3);
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);
        vars[0].setValue(4);
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);

        // Allow backticks for PV name
        f = new Formula("`SomePV` + 3", true);
        vars = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("SomePV", vars[0].getName());
        vars[0].setValue(1);
        assertEquals(4.0, VTypeHelper.toDouble(f.eval()), epsilon);

        // Example from SNS EDM screen conversion
        // Uses '&&', not '&' for 'and'
        f = new Formula("`HEBT_Diag:LES10:EmitHighVolts_RB`==0&&`HEBT_Diag:LES10:MOV_02_axis01_Limit_Retract`==1?0:1", true);
        vars = f.getVariables();
        assertEquals(2, vars.length);
        assertEquals("HEBT_Diag:LES10:EmitHighVolts_RB", vars[0].getName());
        assertEquals("HEBT_Diag:LES10:MOV_02_axis01_Limit_Retract", vars[1].getName());
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);
        vars[0].setValue(0);
        vars[1].setValue(1);
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);

        // Boolean 'or' using '||' notation, one PV
        f = new Formula("`pv0` <= -1  ||  `pv0` > 10", true);
        vars = f.getVariables();
        assertEquals(1, vars.length);
        assertEquals("pv0", vars[0].getName());
        vars[0].setValue(0);
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);
        vars[0].setValue(10);
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);
        vars[0].setValue(11);
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);
        vars[0].setValue(-1);
        assertEquals(1.0, VTypeHelper.toDouble(f.eval()), epsilon);
        vars[0].setValue(-0.5);
        assertEquals(0.0, VTypeHelper.toDouble(f.eval()), epsilon);
    }

    @Test
    public void testStrings() throws Exception
    {
        // String with escaped quotes: ``Hello, "Dolly!"``
        Formula f = new Formula("\"Hello, \\\"Dolly!\\\"\"");
        assertEquals("Hello, \"Dolly!\"", VTypeHelper.toString(f.eval()));

        try
        {
            new Formula(" \"Text without closing quote");
            fail("Didn't catch missing closing quote");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), containsString("quoted"));
        }


        f = new Formula("\"Hello, \" + \"World\"");
        assertEquals("Hello, World", VTypeHelper.toString(f.eval()));
    }

    @Test
    public void testAlarms() throws Exception
    {
	VString dataA = VString.of("a", Alarm.none(), Time.now());
        VString dataB = VString.of("b", Alarm.of(AlarmSeverity.MINOR, AlarmStatus.RECORD, "LOLO"), Time.now());
        VString dataC = VString.of("c", Alarm.of(AlarmSeverity.MAJOR, AlarmStatus.RECORD, "HIHI"), Time.now());
	VariableNode v[] = new VariableNode[3];

        v[0] = new VariableNode("dataA");
        v[1] = new VariableNode("dataB");
	v[2] = new VariableNode("dataC");
        v[0].setValue(dataA);
        v[1].setValue(dataB);
	v[2].setValue(dataC);

	Formula f = new Formula("highestSeverity(dataA, dataB, dataC)", v);
	assertEquals("MAJOR", VTypeHelper.toString(f.eval()));
    }
}
