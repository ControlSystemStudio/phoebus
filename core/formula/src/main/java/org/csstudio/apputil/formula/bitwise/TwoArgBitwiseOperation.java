/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.bitwise;

import java.util.List;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

/** Helper for SPI-provided `long operation(long, long)`
 *  @author Mathis Huriez
 */
@SuppressWarnings("nls")
class TwoArgBitwiseOperation implements FormulaFunction
{
    @FunctionalInterface
    public interface TwoArgOperation
    {
        long calc(long a, long b);
    }

    private final String name;
    private final String description;
    private final TwoArgOperation operation;

    protected TwoArgBitwiseOperation(final String name, final String description, final TwoArgOperation operation)
    {
        this.name = name;
        this.description = description;
        this.operation = operation;
    }

    @Override
    public String getCategory() {
        return "bitwise";
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
    public List<String> getArguments()
    {
        return List.of("x", "y");
    }

    @Override
    public VType compute(final VType... args) throws Exception
    {
        final double arg_a = VTypeHelper.toDouble(args[0]);
        final double arg_b = VTypeHelper.toDouble(args[1]);
        final long a = (long) arg_a, b = (long) arg_b;
        // Check if the conversion is accurate, else, send an exception
        if((double) a != arg_a || (double) b != arg_b)
            throw new Exception("Operation " + getName() +
                    " takes integer types but received floating-point types");
        final long value = operation.calc(a, b);
        return VDouble.of(value, Alarm.none(), Time.now(), Display.none());
    }
}
