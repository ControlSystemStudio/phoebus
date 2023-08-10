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

/** Helper for SPI-provided `long operation(long)`
 *  @author Mathis Huriez
 */
@SuppressWarnings("nls")
class OneArgBitwiseOperation implements FormulaFunction
{
    @FunctionalInterface
    public interface OneArgOperation
    {
        long calc(long arg);
    }

    private final String name;
    private final String description;
    private final OneArgOperation operation;

    protected OneArgBitwiseOperation(final String name, final String description, final OneArgOperation operation)
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
        return List.of("x");
    }

    @Override
    public VType compute(final VType... args) throws Exception
    {
        final double arg = VTypeHelper.toDouble(args[0]);
        final long a = (long) arg;
        if((double) a != arg)
            throw new Exception("Operation " + getName() +
                    " takes integer type but received floating-point type");
        final long value = operation.calc(a);
        return VDouble.of(value, Alarm.none(), Time.now(), Display.none());
    }

}
