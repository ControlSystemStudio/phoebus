/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.macros;

/** Interface that provides a value for a macro
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface MacroValueProvider
{
    /** Get value for macro
     *  @param name Name of the macro
     *  @return Value of the macro or <code>null</code> if not defined
     */
    public String getValue(String name);
}