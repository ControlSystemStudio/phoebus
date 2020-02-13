/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.phoebus.framework.spi.PVProposalProvider;

/** {@link ProposalProvider} for PVA
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PvaProposalProvider implements PVProposalProvider
{
    public static final PvaProposalProvider INSTANCE = new PvaProposalProvider();

    private static final Pattern PLAIN_NAME = Pattern.compile("[a-zA-Z]\\w*");

    private static final List<Proposal> generic = List.of(new Proposal("pva://name"));

    private PvaProposalProvider()
    {
        // Singleton
    }

    @Override
    public String getName()
    {
        return "PV Access PV";
    }

    /** Get proposals
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    @Override
    public List<Proposal> lookup(String text)
    {
        if (text.isEmpty())
            return generic;

        // If user entered a plain name,
        // suggest using it as a pva:// PV
        if (PLAIN_NAME.matcher(text).matches())
            text = "pva://" + text;

        final List<Proposal> result = new ArrayList<>();
        if (text.startsWith("pva"))
        {
            // Just pva..
            if (text.length() <= 6)
                return generic;

            // Plain name turns into pva://,
            // or user already entered pva://...
            result.add(new Proposal(text));

            // If there's at least "pva://" plus one character for the name,
            // mention that it's possible to address subfields
            // unless user already entered a subfield
            if (text.length() > 6  &&  text.indexOf('/', 7) < 0)
            {
                result.add(new Proposal(text + "/subfield"));
                result.add(new Proposal(text + "/subfield/subelement"));
            }
        }
        // else: Not pva://, so don't show PVA proposals
        return result;
    }
}
