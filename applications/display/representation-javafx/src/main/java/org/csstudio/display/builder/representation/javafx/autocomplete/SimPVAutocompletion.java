/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

import java.util.Collection;
import java.util.List;

/** Autocompletion for simulated PVs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimPVAutocompletion extends CollectionBasedAutocompletionProvider
{
    public static final AutocompletionProvider INSTANCE = new SimPVAutocompletion();
    private static final List<Suggestion> pvs = List.of(
        new Suggestion("sim://sine"),
        new Suggestion("sim://sine", "(min, max, update_seconds)"),
        new Suggestion("sim://sine", "(min, max, steps, update_seconds)"),
        new Suggestion("sim://ramp"),
        new Suggestion("sim://ramp", "(min, max, update_seconds)"),
        new Suggestion("sim://ramp", "(min, max, step, update_seconds)"),
        new Suggestion("sim://noise", "(min, max, update_seconds)"),
        new Suggestion("sim://flipflop", "(update_seconds)")
        );

    private SimPVAutocompletion()
    {
        super("Simulated PVs");
    }

    @Override
    protected Collection<Suggestion> getAllEntries()
    {
        return pvs;
    }
}