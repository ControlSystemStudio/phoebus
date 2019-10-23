/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.math;

import org.csstudio.apputil.formula.VTypeHelper;
import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;

/** Helper for SPI-provided `double function(double)`
 *  @author Kay Kasemir
 */
class OneArgMathFunction implements FormulaFunction
{
    @FunctionalInterface
    public interface OneArgFunction
    {
        double calc(double arg);
    }

    private final String name;
    private final String description;
    private final OneArgFunction function;

    protected OneArgMathFunction(final String name, final String description, final OneArgFunction function)
    {
        this.name = name;
        this.description = description;
        this.function = function;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public int getArgumentCount()
    {
        return 1;
    }

    @Override
    public VType compute(final VType... args) throws Exception
    {
        final double arg = VTypeHelper.getDouble(args[0]);
        final double value = function.calc(arg);
        return VDouble.of(value, Alarm.none(), Time.now(), Display.none());
    }
}
