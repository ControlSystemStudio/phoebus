/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.apputil.formula.Formula;
import org.csstudio.apputil.formula.VariableNode;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVListener;
import org.phoebus.pv.PVPool;
import org.phoebus.vtype.VType;

/** Filter that computes alarm enablement from expression.
 *  <p>
 *  Example:
 *  When configured with formula
 *  <pre>2*PV1 > PV2</pre>
 *  Filter will subscribe to PVs "PV1" and "PV2".
 *  For each value change in the input PVs, the formula is
 *  evaluated and the listener is notified of the result.
 *  <p>
 *  When subscribing to PVs, note that the filter uses the same
 *  mechanism as the alarm server, i.e. when the EPICS plug-in
 *  is configured to use 'alarm' subscriptions, the filter PVs
 *  will also only send updates when their alarm severity changes,
 *  NOT for all value changes.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Filter implements PVListener
{
    /** Listener to notify when the filter computes a new value */
    final private Consumer<Double> listener;

    /** Formula to evaluate */
    final private Formula formula;

    /** Variables used in the formula. May be [0], but never null */
    final private VariableNode[] variables;

    /** This array is linked to <code>variables</code>:
     *  Same size, and there's a PV for each VariableNode.
     */
    final private PV pvs[];

    private double previous_value = Double.NaN;

    /** Initialize
     *  @param filter_expression Formula that might contain PV names
     *  @throws Exception on error
     */
    public Filter(final String filter_expression,
                  final Consumer<Double> listener) throws Exception
    {
        this.listener = listener;
        formula = new Formula(filter_expression, true);
        final VariableNode vars[] = formula.getVariables();
        if (vars == null)
            variables = new VariableNode[0];
        else
            variables = vars;

        pvs = new PV[variables.length];
    }

    public String getExpression()
    {
        return formula.getFormula();
    }

    /** Start control system subscriptions */
    public void start() throws Exception
    {
        for (int i=0; i<pvs.length; ++i)
        {
            pvs[i] = PVPool.getPV(variables[i].getName());
            pvs[i].addListener(this);
        }
    }

    /** Stop control system subscriptions */
    public void stop()
    {
        for (int i=0; i<pvs.length; ++i)
        {
            pvs[i].removeListener(this);
            PVPool.releasePV(pvs[i]);
            pvs[i] = null;
        }
    }

    /** @param pv PV used by the formula
     *  @return Associated variable node
     */
    private VariableNode findVariableForPV(final PV pv)
    {
        for (int i=0; i<pvs.length; ++i) // Linear, assuming there are just a few PVs in one formula
            if (pvs[i] == pv)
                return variables[i];
        logger.log(Level.WARNING, "Got update for PV {0} that is not assigned to variable", pv.getName());
        return null;
    }

    /** @see PVListener */
    @Override
    public void valueChanged(final PV pv, final VType value)
    {
        final VariableNode variable = findVariableForPV(pv);
        if (variable == null)
            return;
        final double number = VTypeHelper.toDouble(value);
        logger.log(Level.FINER, () -> { return "Filter " + formula.getFormula() + ": " + pv.getName() + " = " + number; });
        variable.setValue(number);
        evaluate();
    }

    /** @see PVListener */
    @Override
    public void disconnected(final PV pv)
    {
        final VariableNode variable = findVariableForPV(pv);
        if (variable == null)
            return;
        logger.log(Level.WARNING, "PV " + pv.getName() + " (var. " + variable.getName() + ") disconnected");
        variable.setValue(Double.NaN);
        evaluate();
    }

    /** Evaluate filter formula with current input values */
    private void evaluate()
    {
        final double value = formula.eval();
        // Only update on _change_, not whenever inputs send an update
        synchronized (this)
        {
            if (previous_value == value)
                return;
            previous_value  = value;
        }
        listener.accept(value);
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        return "Filter '" + formula.getFormula() + "'";
    }
}
