/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.formula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.csstudio.apputil.formula.Formula;
import org.csstudio.apputil.formula.VariableNode;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;

/** Formula-based {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaPV extends PV
{
    /** Evaluate formulas on one thread
     *  to decouple and throttle input updates
     */
    private static final ExecutorService update_thread = Executors.newSingleThreadExecutor(target ->
    {
        final Thread thread = new Thread(target, "FormulaPV");
        thread.setDaemon(true);
        return thread;
    });

    /** Is there already a pending update? */
    private AtomicBoolean pending = new AtomicBoolean();

    private Formula formula;
    private volatile FormulaInput[] inputs;

    protected FormulaPV(final String name, final String expression)
    {
        super(name);
        try
        {
            // Parse expression...
            formula = new Formula(expression, true);

            final VType value = formula.eval();
            notifyListenersOfValue(value);

            // Determine variables, connect to PVs
            final VariableNode vars[] = formula.getVariables();
            inputs = new FormulaInput[vars.length];
            for (int i=0; i<inputs.length; ++i)
            {   // Initialize 'disconnected' until PV sends first value
                vars[i].setValue(VDouble.of(Double.NaN, Alarm.disconnected(), Time.now(), Display.none()));
                inputs[i] = new FormulaInput(this, vars[i]);
            }

            // Set initial value
            doUpdate();
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

    /** Schedule evaluation of formula */
    void update()
    {
        if (pending.getAndSet(true))
            logger.log(Level.FINE, () -> getName() + " skips recalc on " + Thread.currentThread());
        else
            update_thread.submit(this::doUpdate);
    }

    /** Compute updated value of formula and notify listeners */
    private void doUpdate()
    {
        pending.set(false);
        logger.log(Level.FINE, () -> getName() + " recalc on " + Thread.currentThread());

        // Simulate slow evaluation
        // try { Thread.sleep(100); } catch (InterruptedException e) {}

        final VType value = formula.eval();
        notifyListenersOfValue(value);
    }

    @Override
    protected void close()
    {
        // Close variable PVs
        // Inputs or individual input may be null for formulas that failed to initialize
        if (inputs != null)
            for (FormulaInput input : inputs)
                if (input != null)
                    input.close();
        inputs = null;
    }
}
