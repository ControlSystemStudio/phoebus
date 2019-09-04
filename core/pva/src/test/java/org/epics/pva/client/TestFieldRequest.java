/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.epics.pva.data.Hexdump;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVATypeRegistry;
import org.junit.Test;

@SuppressWarnings("nls")
public class TestFieldRequest
{
    /* Compare to output from IOC running with EPICS_PVA_DEBUG=100
     *
     * Plain 'get'
     * Hexdump [Get request] size = 15
     * 01 00 00 00  01 00 00 00  08    FD 01 00                  80     00 00
     * sid          cid          INIT  FULL_WITH_ID_TYPE_CODE 1  Struct '' empty
     *
     * pvget -r proton_charge
     * Hexdump [Get request] size = 47
     * 01 00 00 00  01 00 00 00  08 FD 01 00  80 00 01 05  .... .... .... ....
     * 66 69 65 6C  64 FD 02 00  80 00 01 0D  70 72 6F 74  fiel d... .... prot
     * 6F 6E 5F 63  68 61 72 67  65 FD 03 00  80 00 00     on_c harg e... ...
     *
     * sid 0, cid 0, INIT,
     * FULL_WITH_ID_TYPE_CODE 1 struct named '', 1 element,
     *     element "field": FULL_WITH_ID_TYPE_CODE 2 struct named '', 1 element
     *         "proton_charge": FULL_WITH_ID_TYPE_CODE 3 struct '' empty
     *
     * pvget -r 'proton_charge,pixel' neutrons
     * pvget -r 'field(proton_charge,pixel)' neutrons
     * Hexdump [Get request] size = 56
     * 01 00 00 00  01 00 00 00  08 FD 01 00  80 00 01 05  .... .... .... ....
     * 66 69 65 6C  64 FD 02 00  80 00 02 0D  70 72 6F 74  fiel d... .... prot
     * 6F 6E 5F 63  68 61 72 67  65 FD 03 00  80 00 00 05  on_c harg e... ....
     * 70 69 78 65  6C FE 03 00                             pixe l...
     * sid 0, cid 0, INIT,
     * FULL_WITH_ID_TYPE_CODE 1 struct named '', 1 element,
     *     element "field": FULL_WITH_ID_TYPE_CODE 2 struct named '', 2 elements
     *         "proton_charge": FULL_WITH_ID_TYPE_CODE 3 struct '' empty
     *         "pixel": ONLY_ID_TYPE_CODE 3
     *
     * pvget -r 'field(proton_charge,pixel,timeStamp.userTag)' neutrons
     * Hexdump [Get request] size = 83
     * 01 00 00 00  01 00 00 00  08 FD 01 00  80 00 01 05  .... .... .... ....
     * 66 69 65 6C  64 FD 02 00  80 00 03 0D  70 72 6F 74  fiel d... .... prot
     * 6F 6E 5F 63  68 61 72 67  65 FD 03 00  80 00 00 05  on_c harg e... ....
     * 70 69 78 65  6C FE 03 00  09 74 69 6D  65 53 74 61  pixe l... .tim eSta
     * 6D 70 FD 04  00 80 00 01  07 75 73 65  72 54 61 67  mp.. .... .use rTag
     * FE 03 00                                            ...
     * FULL_WITH_ID_TYPE_CODE 1 struct named '', 1 element,
     *     element "field": FULL_WITH_ID_TYPE_CODE 2 struct named '', 3 elements
     *         "proton_charge": FULL_WITH_ID_TYPE_CODE 3 struct '' empty
     *         "pixel": ONLY_ID_TYPE_CODE 3
     *         "timeStamp": FULL_WITH_ID_TYPE_CODE 4 struct named '', 1 element,
     *               "userTag": ONLY_ID_TYPE_CODE 3
     *
     *
     * pvmonitor  -r "record[pipeline:1]field(value)" demo
     * CA 02 80 0D 00 00 00 56 00 00 00 01 00 00 00 01 - .......V........
     * 88 FD 00 02 80 00 02 06 72 65 63 6F 72 64 FD 00 - ........record..
     * 03 80 00 01 08 5F 6F 70 74 69 6F 6E 73 FD 00 04 - ....._options...
     * 80 00 01 08 70 69 70 65 6C 69 6E 65 60 05 66 69 - ....pipeline`.fi
     * 65 6C 64 FD 00 05 80 00 01 05 76 61 6C 75 65 FD - eld.......value.
     * 00 06 80 00 00 04 74 72 75 65 00 00 00 02       - ......true....
     *
     * Monitor (0D) SID 1, Request 1,
     * Sub 0x88 (init 08 + pipeline 80),
     * FULL_WITH_ID_TYPE_CODE 2, struct named '', 2 elements,
     *   Element 'record': Type 3, struct named '', 1 element,
     *     Element '_options': Type 4, struct named '', 1 element,
     *       Element 'pipeline': String
     *   Element 'field': Type 5, struct named '', 1 element,
     *     Element 'value': Type 6, struct named '', 0(!) elements
     * Request data: String "true"
     * int nfree: 2
     */
    @Test
    public void testEmpty() throws Exception
    {
        FieldRequest request = new FieldRequest("");
        System.out.println(request);
        ByteBuffer buffer = encode(request);
        System.out.println(Hexdump.toHexdump(buffer));
    }

    @Test
    public void testPlainFields() throws Exception
    {
        FieldRequest request = new FieldRequest("field(proton_charge,pixel)");
        System.out.println(request);
        ByteBuffer buffer = encode(request);
        System.out.println(Hexdump.toHexdump(buffer));
    }

    @Test
    public void testSubFields() throws Exception
    {
        FieldRequest request = new FieldRequest("field(value, timeStamp.userTag)");
        System.out.println(request);
        ByteBuffer buffer = encode(request);
        System.out.println(Hexdump.toHexdump(buffer));

        request = new FieldRequest("field(proton_charge,pixel,timeStamp.userTag)");
        System.out.println(request);
        buffer = encode(request);
        System.out.println(Hexdump.toHexdump(buffer));

        PVATypeRegistry registry = new PVATypeRegistry();
        final PVAData decoded = registry.decodeType("", buffer);
        System.out.println(decoded.formatType());
    }

    @Test
    public void testPipeline() throws Exception
    {
        FieldRequest request = new FieldRequest(10, "field(value)");
        System.out.println(request);
        ByteBuffer buffer = encode(request);
        System.out.println(Hexdump.toHexdump(buffer));
    }

    private ByteBuffer encode(final FieldRequest request) throws Exception
    {
        final ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        request.encodeType(buffer);
        request.encode(buffer);
        buffer.flip();
        return buffer;
    }
}
