/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.command;

/** Comparison used by the {@link WaitCommand}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public enum Comparison
{
    /** Value is at desired value, within tolerance */
    EQUALS("="),

    /** Different from given value */
    UNEQUAL("!="),

    /** Value above desired value, '>' */
    ABOVE(">"),

    /** Value at or above desired value, '>=' */
    AT_LEAST(">="),

    /** Value below desired value, '<' */
    BELOW("<"),

    /** Value at or below desired value, '<=' */
    AT_MOST("<="),

    /** Value has increased by some amount */
    INCREASE_BY("to increase by"),

    /** Value has decreased by some amount */
    DECREASE_BY("to decrease by");

    final private String label;

    /** Initialize
     *  @param label
     */
    private Comparison(final String label)
    {
        this.label = label;
    }

    /** @return Label, i.e. text representation meant for GUI
     *  @see #name()
     */
    @Override
    public String toString()
    {
        return label;
    }
}
