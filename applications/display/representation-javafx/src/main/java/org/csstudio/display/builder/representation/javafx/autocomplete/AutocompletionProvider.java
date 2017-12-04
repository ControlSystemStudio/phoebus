/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

import java.util.List;

/** Autocompletion Provider
 *  @author Kay Kasemir
 */
public interface AutocompletionProvider
{
    /** @return Name of the provider, for example "History" or "Local PV Names" */
    public String getName();

    /** Get suggested entries
     *
     *  <p>Called in background thread,
     *  may take some time to return data.
     *
     *  <p>Will be interrupted if a new request
     *  makes the ongoing request obsolete.
     *
     *  @param text Text entered by the user
     *  @return Suitable {@link Suggestion}s
     */
    public List<Suggestion> getEntries(String text);
}