/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

/** One autocompletion suggestion
 *
 *  <p>The 'value' is what's actually used
 *  when the user selectes this suggestion.
 *
 *  <p>The 'comment' is shown in the list of suggestions,
 *  for example to explain parameters, but it's not
 *  included in the value.
 *
 *  @author Kay Kasemir
 */
public class Suggestion
{
    private final String value, comment;

    /** @param value Value */
    public Suggestion(final String value)
    {
        this(value, null);
    }

    /** @param value Value
     *  @param comment Comment
     */
    public Suggestion(final String value, final String comment)
    {
        this.value = value;
        this.comment = comment;
    }

    /** @return Value, i.e. the text that should be used when accepting this suggestion */
    public String getValue()
    {
        return value;
    }

    /** @return Comment, for example parameter information, to show with the suggestion */
    public String getComment()
    {
        return comment;
    }

    @Override
    public String toString()
    {
        if (comment == null)
            return value;
        return value + " (" + comment + ")";
    }
}