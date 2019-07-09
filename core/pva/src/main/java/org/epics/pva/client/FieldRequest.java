/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.epics.pva.data.PVABool;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAStructure;

/** Description of the 'field(..)' request used to get/monitor a channel
 *
 *  <p>Supported requests:
 *
 *  <p>"", "field()":
 *  Empty request that fetches the complete structure.
 *
 *  <p>"value,other", "field(value, other)":
 *  Request specific fields.
 *
 *  <p>"value, timeStamp.userTag", "field(value, timeStamp.userTag)":
 *  Request specific fields, addressing an element of a sub-structure.
 *
 *  <p>The size of the encoded request depends not only on the content
 *  per se but specifically on how often a type is re-used within the
 *  request.
 *  At this time, the only way to determine the size is by encoding it,
 *  and then checking the resulting size.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class FieldRequest
{
    private final PVAStructure desc;

    /** Parse field request
     *  @param request Examples:
     *                 "", "field()",
     *                 "value", "field(value)",
     *                 "field(value, timeStamp.userTag)"
     */
    public FieldRequest(final String request)
    {
        this(0, request);
    }

    /** Parse field request
     *  @param pipeline Number of elements for 'pipeline' mode, 0 to disable
     *  @param request Examples:
     *                 "", "field()",
     *                 "value", "field(value)",
     *                 "field(value, timeStamp.userTag)"
     */
    public FieldRequest(final int pipeline, final String request)
    {
        final List<PVAData> items = new ArrayList<>();

        if (pipeline > 0)
        {
            // record._options.pipeline=true
            // 'camonitor' encodes as PVAString 'true', not PVABool
            items.add(
                new PVAStructure("record", "",
                    new PVAStructure("_options", "",
                        new PVABool("pipeline", true),
                        new PVAInt("queueSize", pipeline)
                        )));
        }

        // XXX Not using any client type registry,
        //     but (re-)defining from 1 each time
        // [#1] : desc
        // [#2] : desc.field
        // [#3] : All the plain elements
        // [#4, 5, ..]: Additional sub-structs
        final List<String> fields = parseFields(request);
        if (! fields.isEmpty())
        {
            final List<PVAData> field_elements = new ArrayList<>(fields.size());
            final AtomicInteger struct_id = new AtomicInteger(4);
            for (String field : fields)
                field_elements.add(createFieldElement(field, struct_id));
            final PVAStructure field_struct = new PVAStructure("field", "", field_elements);
            items.add(field_struct);
            field_struct.setTypeID((short)2);
        }

        desc = new PVAStructure("", "", items);
        desc.setTypeID((short)1);
    }

    /** @param "a, b.sub"
     *  @return [ "a", "b.sub" ]
     */
    private List<String> parseFields(final String field_spec)
    {
        final List<String> fields = new ArrayList<>();
        final int length = field_spec.length();
        int pos = field_spec.indexOf("field(");
        if (pos < 0)
            pos = 0;
        else
            pos += 6;
        while (pos < length)
        {
            char c = field_spec.charAt(pos);
            // Skip space
            if (Character.isWhitespace(c))
                ++pos;
            // End of field(...)?
            else if (c == ')')
                break;
            else
            {   // Start of field.
                // Find its end
                int end = pos+1;
                while (end < length)
                {
                    char cc = field_spec.charAt(end);
                    if (Character.isWhitespace(cc))
                        break;
                    else if (cc == ','  ||  cc == ')')
                        break;
                    else
                        ++end;
                }
                if (end > pos)
                    fields.add(field_spec.substring(pos, end));
                pos = end+1;
            }
        }
        return fields;
    }

    /** @param field "a" or "a.sub.subsub"
     *  @param struct_id Next free struct ID
     *  @return `structure a` or struct with sub-elements
     */
    private PVAData createFieldElement(final String field, final AtomicInteger struct_id)
    {
        final String[] sub = field.split("\\.");
        int i = sub.length-1;
        PVAStructure result = new PVAStructure(sub[i], "");
        result.setTypeID((short)3);
        while (--i  >= 0)
        {
            result = new PVAStructure(sub[i], "", result);
            result.setTypeID((short)struct_id.getAndIncrement());
        }
        return result;
    }

    /** Encode the request description
     *
     *  @param buffer {@link ByteBuffer}
     *  @throws Exception on error
     */
    public void encodeType(final ByteBuffer buffer) throws Exception
    {
        final BitSet described = new BitSet();
        desc.encodeType(buffer, described);
    }

    /** Encode the request value (pipeline option)
     *
     *  @param buffer {@link ByteBuffer}
     *  @throws Exception on error
     */
    public void encode(final ByteBuffer buffer) throws Exception
    {
        desc.encode(buffer);
    }

    @Override
    public String toString()
    {
        return desc.format();
    }
}
