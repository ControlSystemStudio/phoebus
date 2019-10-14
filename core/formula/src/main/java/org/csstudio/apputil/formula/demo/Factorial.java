/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.demo;

import org.csstudio.apputil.formula.VTypeHelper;
import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VInt;
import org.epics.vtype.VType;

/** Example for SPI-provided function
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Factorial implements FormulaFunction
{
    @Override
    public String getName()
    {
        return "fac";
    }

    @Override
    public int getArgumentCount()
    {
        return 1;
    }

    @Override
    public VType compute(VType... args) throws Exception
    {
        final int n = (int) VTypeHelper.getDouble(args[0]);
        return VInt.of(fac(n), Alarm.none(), Time.now(), Display.displayOf(args[0]));
    }

    private static int fac(final int n)
    {
        if (n <= 1)
            return 1;
        return n * fac(n-1);
    }
}
