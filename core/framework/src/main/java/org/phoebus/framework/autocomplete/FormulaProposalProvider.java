/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.phoebus.framework.spi.PVProposalProvider;

/** Provider of proposals for formulas
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaProposalProvider implements PVProposalProvider
{
    public static final FormulaProposalProvider INSTANCE = new FormulaProposalProvider();

    private final List<Proposal> generic = List.of(new Proposal("=2*`pv_name`"));

    private final List<FormulaFunctionProposal> functions = new ArrayList<>();

    private FormulaProposalProvider()
    {
        for (FormulaFunction func : ServiceLoader.load(FormulaFunction.class))
            functions.add(new FormulaFunctionProposal(func));
    }

    @Override
    public String getName()
    {
        return "Formula";
    }

    /** Get proposals
     *
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    @Override
    public List<Proposal> lookup(final String text)
    {
        // User is not entering a formula: Just hint that they exist
        if (! text.startsWith("="))
            return generic;

        // User is typing a formula
        // Find all functions where the text results in a match
        final List<Proposal> result = new ArrayList<>();
        for (Proposal p : functions)
            for (MatchSegment seg : p.getMatch(text))
                if (seg.getType() == MatchSegment.Type.MATCH)
                {
                    result.add(p);
                    break;
                }

        // Show functions with a match
        if (result.size() > 0)
            return result;

        // No function matches, so list all of them
        result.clear();
        result.addAll(generic);
        result.addAll(functions);

        return result;
    }
}
