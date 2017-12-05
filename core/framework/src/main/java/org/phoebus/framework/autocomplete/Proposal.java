/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

/** Basic value-based proposal
 *
 *  <p>Holds a plain string,
 *  which will be used to replace entered text.
 *
 *  @author Kay Kasemir
 */
public class Proposal
{
    protected final String value;

    /** @param value Value of the proposal */
    public Proposal(final String value)
    {
        this.value = value;
    }

    /** @return value Value of the proposal */
    public String getValue()
    {
        return value;
    }

    /** @return Text to display when showing the proposal for user to select it */
    public String getDescription()
    {
        return value;
    }

    // TODO Get info to highlight 'match'

    /** Apply the proposal
     *
     *  @param text Original text entered by user
     *  @return Result of applying this proposal to the text
     */
    public String apply(final String text)
    {
        return value;
    }
}
