/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.apputil.formula.Formula;
import org.csstudio.apputil.formula.VariableNode;
import org.epics.vtype.VType;
import org.phoebus.framework.jobs.NamedThreadFactory;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import org.phoebus.core.vtypes.VTypeHelper;

import io.reactivex.disposables.Disposable;

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
public class Filter
{
    /** Timer shared by all filters */
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("FilterEvaluation"));

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

    private final Disposable[] flows;

    private volatile double current_value = Double.NaN;

    /** Is a call to evaluate() pending? */
    private final AtomicBoolean evaluation_pending = new AtomicBoolean();

    private class FilterPVhandler implements io.reactivex.functions.Consumer<VType>
    {
        private final int index;

        FilterPVhandler(final int index)
        {
            this.index = index;
        }

        @Override
        public void accept(final VType value)
        {
            if (PV.isDisconnected(value))
            {
                logger.log(Level.WARNING, "PV " + pvs[index].getName() + " (var. " + variables[index].getName() + ") disconnected");
                variables[index].setValue(Double.NaN);
            }
            else
            {
                final double number = VTypeHelper.toDouble(value);
                logger.log(Level.FINER, () -> { return "Filter " + formula.getFormula() + ": " + pvs[index].getName() + " = " + number; });
                variables[index].setValue(number);
            }

            // If an evaluation has not already been scheduled, do it
            if (! evaluation_pending.getAndSet(true))
                TIMER.schedule(Filter.this::evaluate, 100, TimeUnit.MILLISECONDS);
        }
    }

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
        flows = new Disposable[variables.length];
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
            // Pass value update ASAP,
            // except when there are multiple rapid updates,
            // in which case the latest is passed after some time.
            // Pass the last known value on close.
            pvs[i] = PVPool.getPV(variables[i].getName());
            flows[i] = pvs[i].onValueEvent()
                             .throttleLatest(500, TimeUnit.MILLISECONDS, true)
                             .subscribe(new FilterPVhandler(i));
        }
    }

    /** Stop control system subscriptions */
    public void stop()
    {
        for (int i=0; i<pvs.length; ++i)
        {
            flows[i].dispose();
            PVPool.releasePV(pvs[i]);
            pvs[i] = null;
        }
    }

    /** Evaluate filter formula with current input values */
    private void evaluate()
    {
        evaluation_pending.set(false);

        final double value = VTypeHelper.toDouble(formula.eval());

        // This code is executed on the single TIMER thread, i.e. serialized
        // No need to synchronize on current_value.
        // Only update on _change_, not whenever inputs send an update
        logger.log(Level.FINER, () -> "Filter evaluates to " + value + " (previous value " + current_value + ") on " + Thread.currentThread());
        if (current_value == value)
            return;

        current_value  = value;
        listener.accept(value);
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Filter '").append(formula.getFormula()).append("'");
        for (PV pv : pvs)
            if (pv != null)
                buf.append(", PV ").append(pv.getName()).append(" = ").append(pv.read());
        return buf.toString();
    }
}
