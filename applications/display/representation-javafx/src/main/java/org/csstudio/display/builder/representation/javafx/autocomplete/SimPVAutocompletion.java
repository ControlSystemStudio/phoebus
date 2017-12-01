/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

import java.util.Collection;

import com.sun.tools.javac.util.List;

/** Autocompletion for simulated PVs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimPVAutocompletion extends CollectionBasedAutocompletionProvider
{
    public static final AutocompletionProvider INSTANCE = new SimPVAutocompletion();
    private static final List<String> pvs = List.of(
        "sim://sine",
        "sim://sine(min, max, update_seconds)",
        "sim://sine(min, max, steps, update_seconds)",
        "sim://ramp",
        "sim://ramp(min, max, update_seconds)",
        "sim://ramp(min, max, step, update_seconds)",
        "sim://noise",
        "sim://flipflop",
        "sim://flipflop(update_seconds)"
        );

    private SimPVAutocompletion()
    {
        super("Simulated PVs");
    }

    @Override
    protected Collection<String> getAllEntries()
    {
        return pvs;
    }
}