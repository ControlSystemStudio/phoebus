/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

/** Description of a match between entered text and a {@link Proposal}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MatchSegment
{
    public static enum Type
    {
        /** Text from proposal which does not appear in entered text.
         *  If proposal is applied, this text will be used.
         */
        NORMAL,

        /** Text from proposal which appears in entered text.
         *  If proposal is applied, this text will remain unchanged,
         *  because it already matches the proposal.
         */
        MATCH,

        /** Text from proposal which is a comment, for example to describe parameters.
         *  If proposal is applied, the original text will remain unchanged.
         */
        COMMENT
    }

    private final String text;
    private final String description;
    private final Type type;

    private MatchSegment(final String text, final Type type)
    {
        this(text, text, type);
    }

    private MatchSegment(final String text, final String description, final Type type)
    {
        this.text = text;
        this.description = description;
        this.type = type;
    }

    /** @return Text segment from the proposal */
    public String getText()
    {
        return text;
    }

    /** @return Description of the test segment. May match text, or be a parameter name for the text */
    public String getDescription()
    {
        return description;
    }

    /** @return Information about that text segment from proposal */
    public Type getType()
    {
        return type;
    }

    /** @param text Text segment from proposal
     *  @return {@link Type#NORMAL} segment
     */
    public static MatchSegment normal(final String text)
    {
        return new MatchSegment(text, Type.NORMAL);
    }

    /** @param text Text segment from proposal
     *  @return {@link Type#MATCH} segment
     */
    public static MatchSegment match(final String text)
    {
        return new MatchSegment(text, Type.MATCH);
    }

    /** @param text Text segment from proposal
     *  @param description Description for text
     *  @return {@link Type#MATCH} segment
     */
    public static MatchSegment match(final String text, final String description)
    {
        return new MatchSegment(text, description, Type.MATCH);
    }

    /** @param text Text segment from proposal
     *  @return {@link Type#COMMENT} segment
     */
    public static MatchSegment comment(final String text)
    {
        return new MatchSegment(text, Type.COMMENT);
    }

    /** @param text Text segment from proposal
     *  @param description Description for text
     *  @return {@link Type#COMMENT} segment
     */
    public static MatchSegment comment(final String text, final String description)
    {
        return new MatchSegment(text, description, Type.COMMENT);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof MatchSegment))
            return false;
        final MatchSegment other = (MatchSegment) obj;
        return type == other.type       &&
               text.equals(other.text)  &&
               description.equals(other.description);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(type.name())
           .append(" '").append(text).append("'");
        if (! text.equals(description))
            buf.append(" - '").append(description).append("'");
        return buf.toString();
    }
}
