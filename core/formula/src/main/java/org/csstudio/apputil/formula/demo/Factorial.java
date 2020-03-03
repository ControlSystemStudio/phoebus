/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.demo;

import java.util.List;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VInt;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

/** Example for SPI-provided function
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Factorial implements FormulaFunction
{

    @Override
    public String getCategory() {
        return "math";
    }

    @Override
    public String getName()
    {
        return "fac";
    }

    @Override
    public String getDescription()
    {
        return "Calculate the factorial of the given number";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("n");
    }

    @Override
    public VType compute(final VType... args) throws Exception
    {
        final int n = (int) VTypeHelper.toDouble(args[0]);
        return VInt.of(fac(n), Alarm.none(), Time.now(), Display.displayOf(args[0]));
    }

    private static int fac(final int n)
    {
        if (n <= 1)
            return 1;
        return n * fac(n-1);
    }

}
