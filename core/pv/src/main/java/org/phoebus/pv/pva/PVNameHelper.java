/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helper for analyzing PV Names
 *
 *  <p>Handles these types of names:
 *  <pre>
 *  pva://channel_name
 *  pva://channel_name?request=field(some.structure.element)
 *  pva://channel_name/some/structure.element
 *  pva://channel_name/some/array/structure.element[index]
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVNameHelper
{
    final private static Pattern REQUEST_PATTERN = Pattern.compile("\\?request=field\\((.*)\\)");
    final private static int REQUEST_FIELD_START = "?request=".length();

    // Regular expression for parsing out the array index value if present
    final private static Pattern ARRAY_PATTERN = Pattern.compile(".*(\\[\\S*\\])$");
    final private static Pattern ARRAY_INDEX_PATTERN = Pattern.compile("\\[(\\d*)\\]$");
    
    final private String channel, field, read, write;
    final private Optional<Integer> elementIndex;

    /** Create parser
     *
     *  @param pv_name PV name
     *  @return {@link PVNameHelper}
     *  @throws Exception on error
     */
    public static PVNameHelper forName(final String pv_name) throws Exception
    {
        // PV name that follow pvget/eget URL syntax can be
        // "pva:///the_name" with 3 '///' to allow for a "pva://host:port/the_name".
        // Strip the 3rd '/'
        final String name = pv_name.startsWith("/") ? pv_name.substring(1) : pv_name;
        // Does name include "?request.."?
        int pos = name.indexOf('?');
        if (pos >= 0)
            return PVNameHelper.forNameWithRequest(name.substring(0, pos), name.substring(pos));
        // Does name include "/some/path"?
        pos = name.indexOf('/');
        if (pos >= 0)
            return PVNameHelper.forNameWithPath(name.substring(0, pos), name.substring(pos+1));
        // Plain channel name
        return PVNameHelper.forPlainName(name);
    }

    /** @param channel Channel name
     *  @param request "?request..."
     *  @return {@link PVNameHelper}
     *  @throws Exception on error
     */
    private static PVNameHelper forNameWithRequest(final String channel, final String request) throws Exception
    {
        final Matcher matcher = REQUEST_PATTERN.matcher(request);
        if (! matcher.matches())
            throw new Exception("Expect ?request=field(...) but got \"" + request + "\"");
        String field = matcher.group(1);
        final Optional<Integer> elementIndex;
        final String write;
        if (field.isEmpty())
        {
            field = "value";
            write = "field(value)";
            elementIndex = Optional.empty();
        }
        else
        {
            // Check if the channel name ends with "[index]", if present extract the array
            // index part from the channel name and use it to populate the elementIndex optional
            final Matcher arraySyntax = ARRAY_PATTERN.matcher(field);
            if(arraySyntax.matches())
            {
                String arraySyntaxString = arraySyntax.group(1);
                field = field.substring(0, arraySyntax.start(1));
                final Matcher arrayIndex = ARRAY_INDEX_PATTERN.matcher(arraySyntaxString);
                if(arrayIndex.matches())
                {
                    elementIndex = Optional.of(Integer.valueOf(arrayIndex.group(1)));
                }
                else
                {
                    // Error, the array index has not be correct defined, the index consist of non digit chars
                    throw new Exception("Expect [index], where the index is a valid int but got \"" + field + "\"");
                }
            }
            else
            {
                elementIndex = Optional.empty();
            }
            write = "field(" + field + ".value)";
        }
        return new PVNameHelper(channel, field, elementIndex, request.substring(REQUEST_FIELD_START), write);
    }

    /** @param channel Channel name
     *  @param path "to/some/element" (without initial '/')
     *  @return {@link PVNameHelper}
     *  @throws Exception on error
     */
    private static PVNameHelper forNameWithPath(final String channel, final String path) throws Exception
    {
        String field = path.replace('/', '.');
        final String write;
        final Optional<Integer> elementIndex;
        if(field.isEmpty())
        {
            write = "field(value)";
            elementIndex = Optional.empty();
        }
        else
        {
            final Matcher arraySyntax = ARRAY_PATTERN.matcher(field);
            if(arraySyntax.matches())
            {
                String arraySyntaxString = arraySyntax.group(1);
                field = field.substring(0, arraySyntax.start(1));
                final Matcher arrayIndex = ARRAY_INDEX_PATTERN.matcher(arraySyntaxString);
                if(arrayIndex.matches())
                {
                    elementIndex = Optional.of(Integer.valueOf(arrayIndex.group(1)));
                }
                else
                {
                    // Error, the array index has not be correct defined, the index consist of non digit chars
                    throw new Exception("Expect [index], where the index is a valid int but got \"" + field + "\"");
                }
            }
            else
            {
                elementIndex = Optional.empty();
            }
            write = "field(" + field + ".value)";
        }
        return new PVNameHelper(channel, field, elementIndex,"field(" + field + ")",  write);
    }

    /**
     * @param channel plain channel name w/t array index
     * @return {@link PVNameHelper}
     * @throws Exception on invalid channel name
     */
    private static PVNameHelper forPlainName(String channel) throws Exception
    {
        final Matcher arraySyntax = ARRAY_PATTERN.matcher(channel);
        final Optional<Integer> elementIndex;
        if(arraySyntax.matches())
        {
            String arraySyntaxString = arraySyntax.group(1);
            channel = channel.substring(0, arraySyntax.start(1));
            final Matcher arrayIndex = ARRAY_INDEX_PATTERN.matcher(arraySyntaxString);
            if(arrayIndex.matches())
            {
                elementIndex = Optional.of(Integer.valueOf(arrayIndex.group(1)));
            }
            else
            {
                // Error, the array index has not be correct defined, the index consist of non digit chars
                throw new Exception("Expect [index], where the index is a valid int but got \"" + channel + "\"");
            }
        }
        else
        {
            elementIndex = Optional.empty();
        }
        return new PVNameHelper(channel, "value", elementIndex, "field()", "field(value)");
    }

    /** Private to enforce use of <code>forName</code> */
    private PVNameHelper(final String channel, final String field, final Optional<Integer> elementIndex, final String read, final String write) throws Exception
    {
        if (channel.isEmpty())
            throw new Exception("Empty channel name");
        this.channel = channel;
        this.field = field;
        this.elementIndex = elementIndex;
        this.read = read;
        this.write = write;
    }

    /** @return Channel name */
    public String getChannel()
    {
        return channel;
    }

    /** @return Plain 'value' or 'struct.sub.value' */
    public String getField()
    {
        return field;
    }

    /** @return Request "field(..)" for reading */
    public String getReadRequest()
    {
        return read;
    }

    /** @return Request "field(..)" for writing */
    public String getWriteRequest()
    {
        return write;
    }

    /** @return The element index in the subscribed array*/
    public Optional<Integer> getElementIndex()
    {
        return elementIndex;
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Channel '").append(channel).append("'");
        sb.append(", field '").append(field).append("'");
        if(elementIndex.isPresent())
            sb.append(", element index '").append(String.valueOf(elementIndex.get())).append("'");
        sb.append(", read request '").append(read).append("'");
        sb.append(", write request '").append(write).append("'");
        return sb.toString();
    }
}
