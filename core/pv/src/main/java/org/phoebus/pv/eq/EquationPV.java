/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.eq;

import java.util.logging.Level;

import org.csstudio.apputil.formula.Formula;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VString;
import org.phoebus.pv.PV;

/** Equation-based {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EquationPV extends PV
{
    private final Formula formula;

    protected EquationPV(final String expression)
    {
        super(expression);
        formula = parse(expression);
    }

    private Formula parse(final String expression)
    {
        try
        {
            // Parse expression...
            final Formula formula = new Formula(expression, true);

            // TODO Determine PVs, connect, ..

            // Set initial value
            final double value = formula.eval();
            notifyListenersOfValue(VDouble.of(value, Alarm.none(), Time.now(), Display.none()));
            return formula;
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Formula PV error in " + expression, ex);
            // Set initial value
            notifyListenersOfValue(VString.of(ex.getMessage(), Alarm.noValue(), Time.now()));
            return null;
        }
    }
}
