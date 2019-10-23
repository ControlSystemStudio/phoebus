/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.formula;

import static org.phoebus.pv.PV.logger;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.apputil.formula.VariableNode;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import io.reactivex.disposables.Disposable;

/** Input to a formula
 *
 *  <p>Reads a PV, updates formula variable
 *  and triggers formula evaluation.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class FormulaInput
{
    private final FormulaPV formula_pv;
    private final VariableNode variable;
    private volatile PV pv;
    private volatile Disposable subscription;

    protected FormulaInput(final FormulaPV formula_pv, final VariableNode variable) throws Exception
    {
        this.formula_pv = formula_pv;
        this.variable = variable;
        pv = PVPool.getPV(variable.getName());
        subscription = pv.onValueEvent()
                         .throttleLatest(FormulaPVPreferences.throttle_ms, TimeUnit.MILLISECONDS)
                         .subscribe(this::handleUpdate);
    }

    PV getPV()
    {
        return pv;
    }

    private void handleUpdate(final VType value)
    {
        logger.log(Level.FINE, () -> formula_pv.getName() + " updated by " + pv);
        variable.setValue(value);
        formula_pv.update();
    }

    protected void close()
    {
        if (subscription != null)
        {
            subscription.dispose();
            subscription = null;
        }
        if (pv != null)
        {
            PVPool.releasePV(pv);
            pv = null;
        }
    }
}
