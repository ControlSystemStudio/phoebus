/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.formula;

import java.util.logging.Level;

import org.csstudio.apputil.formula.Formula;
import org.csstudio.apputil.formula.VariableNode;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VString;
import org.phoebus.pv.PV;

/** Formula-based {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class FormulaPV extends PV
{
    private Formula formula;
    private volatile FormulaInput[] inputs;

    protected FormulaPV(final String name, final String expression)
    {
        super(name);
        try
        {
            // Parse expression...
            formula = new Formula(expression, true);

            // Set initial value
            final double value = formula.eval();
            notifyListenersOfValue(VDouble.of(value, Alarm.none(), Time.now(), Display.none()));

            // Determine variables, connect to PVs
            final VariableNode vars[] = formula.getVariables();
            inputs = new FormulaInput[vars.length];
            for (int i=0; i<inputs.length; ++i)
                inputs[i] = new FormulaInput(this, vars[i]);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Formula PV error in " + expression, ex);
            // Set initial value
            notifyListenersOfValue(VString.of(ex.getMessage(), Alarm.noValue(), Time.now()));
        }
    }

    /** Compute updated value of formula and notify listeners */
    void update()
    {
        final double value = formula.eval();
        if (Double.isNaN(value))
            notifyListenersOfValue(VDouble.of(value, Alarm.disconnected(), Time.now(), Display.none()));
        else
            notifyListenersOfValue(VDouble.of(value, Alarm.none(), Time.now(), Display.none()));
    }

    @Override
    protected void close()
    {
        // Close variable PVs
        for (FormulaInput input : inputs)
            input.close();
        inputs = null;
    }
}
