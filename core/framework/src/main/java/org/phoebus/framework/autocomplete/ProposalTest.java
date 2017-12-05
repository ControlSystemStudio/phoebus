/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

@SuppressWarnings("nls")
public class ProposalTest
{
    @Test
    public void testPlainProposal()
    {
        // Plain proposal replaces what was entered
        Proposal proposal = new Proposal("Test1");
        assertThat(proposal.apply("est"),      equalTo("Test1"));
        assertThat(proposal.apply("anything"), equalTo("Test1"));
    }
}
