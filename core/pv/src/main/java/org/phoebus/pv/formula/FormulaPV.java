/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.formula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.apputil.formula.Formula;
import org.csstudio.apputil.formula.VariableNode;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.phoebus.pv.PV;

/** Formula-based {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaPV extends PV
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

            final ListNumber value = formula.eval();
            if (value.size() == 1)
                notifyListenersOfValue(VDouble.of(value.getDouble(0), Alarm.none(), Time.now(), Display.none()));
            else
                notifyListenersOfValue(VNumberArray.of(value, Alarm.none(), Time.now(), Display.none()));

            // Determine variables, connect to PVs
            final VariableNode vars[] = formula.getVariables();
            inputs = new FormulaInput[vars.length];
            for (int i=0; i<inputs.length; ++i)
                inputs[i] = new FormulaInput(this, vars[i]);

            // Set initial value
            update();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Formula PV error in " + expression, ex);
            // Set initial value
            notifyListenersOfValue(VString.of(ex.getMessage(), Alarm.noValue(), Time.now()));
        }
    }

    /** @return Formula expression */
    public String getExpression()
    {
        return formula.getFormula();
    }

    /** @return PVs that are inputs to this formula */
    public Collection<PV> getInputs()
    {
        final List<PV> pvs = new ArrayList<>(inputs.length);
        for (FormulaInput input : inputs)
            pvs.add(input.getPV());
        return pvs;
    }

    /** Compute updated value of formula and notify listeners */
    void update()
    {
        final ListNumber value = formula.eval();
        if (value.size() == 1)
        {
            if (Double.isNaN(value.getDouble(0)))
                notifyListenersOfValue(VDouble.of(Double.NaN, Alarm.disconnected(), Time.now(), Display.none()));
            else
                notifyListenersOfValue(VDouble.of(value.getDouble(0), Alarm.none(), Time.now(), Display.none()));
        }
        else
            notifyListenersOfValue(VNumberArray.of(value, Alarm.none(), Time.now(), Display.none()));
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
