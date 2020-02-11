/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.spi;

import java.util.List;
import java.util.stream.Collectors;

import org.epics.vtype.VType;

/** SPI for contributing a function to the formula
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public interface FormulaFunction
{

    /** @return Name of the Category the function belongs to*/
    public String getCategory();

    /** @return Name of the function as used in the formula */
    public String getName();

    /** @return A description on the function performed by the formula */
    public String getDescription();

    /** @return Description of arguments, for example `[ "base", "exponent" ]` */
    public List<String> getArguments();

    /** Compute the function's value
     *  @param args Arguments, count will match <code>getArgumentCount()</code>
     *  @return Value
     *  @throws Exception on error
     */
    public VType compute(VType... args) throws Exception;

    /** @return "function(arg1, arg2)"
     */
    public default String getSignature()
    {
        return getName() + "(" + getArguments().stream().collect(Collectors.joining(",")) + ")";
    }
}
