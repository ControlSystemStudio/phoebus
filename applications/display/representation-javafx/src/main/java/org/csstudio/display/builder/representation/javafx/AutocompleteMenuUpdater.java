/*******************************************************************************
 * Copyright (c) 2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

/** Generic updater interface for {@link AutocompleteMenu}.
 *
 *  <p>Install via {@link AutocompleteMenu#setUpdater}.
 *  Auto complete menu will then call this interface
 *  to request autocomplete suggestions.
 *
 *  @author Amanda Carpenter
 */
public interface AutocompleteMenuUpdater
{
    /** Request autocomplete entries.
     *
     *  <p>Called by {@link AutocompleteMenu} when user enters text.
     *  Should locate suitable suggestions,
     *  then call {@link AutocompleteMenu#setResults}
     *  to display them.
     *
     *  <p>Must return 'immediately', so would usually
     *  start background job to perform the lookup,
     *  and then call {@link AutocompleteMenu#setResults} from the
     *  background job.
     *
     *  <p>When called while a previous background job is still
     *  running, it should cancel the previous one and
     *  start a new lookup for the current content.
     *
     *  @param content Content for which to request autocomplete entries
     */
    public void requestEntries(String content);

    /** Add the given entry into autocomplete history.
     *
     *  @param entry Entry to be added to history
     */
    public void updateHistory(String entry);
}
