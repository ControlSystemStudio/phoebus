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

        // Does text contain parameters?
        final String sub_text = text.substring(1);
        final List<String> split = SimProposal.splitNameAndParameters(sub_text);
        final String noparm_text = split.get(0);
        final int given = SimProposal.hasOpeningBacket(sub_text)
                ? split.size() - 1
                : -1;
        final boolean complete_args = SimProposal.hasClosingBacket(sub_text);

        // Search 'all' proposals for match
        final List<Proposal> result = new ArrayList<>();
        // First compare text up to optional parameters
        for (FormulaFunctionProposal proposal : functions)
            if (proposal.getName().toLowerCase().contains(noparm_text.toLowerCase())) {
                // If text contains arguments, check them
                if (given >= 0) {
                    final int required = proposal.getArguments().length;
                    // Skip if text contains more arguments than proposal allows
                    if (given > required)
                        continue;
                    // Skip if text contains complete arguments "(...)" but wrong number
                    if (given != required && complete_args)
                        continue;
                    // Text has fewer arguments, or not ending in "..)"
                }
                result.add(proposal);
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
