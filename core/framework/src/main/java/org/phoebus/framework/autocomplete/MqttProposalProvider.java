/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ArrayList;
import java.util.List;

import org.phoebus.framework.spi.PVProposalProvider;

/** Provider of {@link MqttProposal}s
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MqttProposalProvider implements PVProposalProvider
{
    public static final MqttProposalProvider INSTANCE = new MqttProposalProvider();

    private static final List<Proposal> generic = List.of(new MqttProposal("mqtt://path", "VType"));

    private MqttProposalProvider()
    {
        // Singleton
    }

    @Override
    public String getName()
    {
        return "MQTT PV";
    }

    /** Get proposals
     *
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    @Override
    public List<Proposal> lookup(final String text)
    {
        if (! text.startsWith("mqtt://"))
            return generic;

        final List<Proposal> result = new ArrayList<>();
        final List<String> split = MqttProposal.splitPathType(text);

        // Use the entered name, but add "mqtt://".
        // Default to just "mqtt://path"
        String name = split.get(0).trim();
        if (name.isEmpty())
            name = "mqtt://path";
        else if (! name.startsWith("mqtt://"))
            name = "mqtt://" + name;

        // Use the entered type, or default to "VType"
        String type = split.get(1);
        if (type != null)
        {
            result.add(new MqttProposal(name, "VDouble"));
            result.add(new MqttProposal(name, "VString"));
        }
        else
            result.add(new MqttProposal(name, "VType"));
        return result;
    }
}
