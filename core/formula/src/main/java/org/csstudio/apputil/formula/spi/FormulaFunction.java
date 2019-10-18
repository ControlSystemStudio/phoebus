/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.spi;

import org.epics.vtype.VType;

/** SPI for contributing a function to the formula
 *  @author Kay Kasemir
 */
public interface FormulaFunction
{
    /** @return Name of the function as used in the formula */
    public String getName();

    /** @return A description on the function performed by the formula */
    public String getDescription();

    /** @return Number of arguments with which the function needs to be called */
    public int getArgumentCount();

    /** Compute the function's value
     *  @param args Arguments, count will match <code>getArgumentCount()</code>
     *  @return Value
     *  @throws Exception on error
     */
    public VType compute(VType... args) throws Exception;
}
