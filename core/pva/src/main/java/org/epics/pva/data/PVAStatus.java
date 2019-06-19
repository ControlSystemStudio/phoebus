/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.nio.ByteBuffer;
import java.util.Objects;

/** Encode/decode PVA Status
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAStatus
{
    public static final PVAStatus StatusOK = new PVAStatus(Type.OK, "", "");

    public enum Type
    {
        OK,
        WARNING,
        ERROR,
        FATAL;
    }

    public final Type type;
    public final String message;
    public final String stacktrace;

    public PVAStatus(final Type type, final String message, final String stacktrace)
    {
        this.type = type;
        this.message = Objects.requireNonNull(message);
        this.stacktrace = Objects.requireNonNull(stacktrace);
    }

    /** @return OK or WARNING? */
    public boolean isSuccess()
    {
        return type == Type.OK  ||  type == Type.WARNING;
    }

    public void encode(final ByteBuffer buffer)
    {
        // When OK and no message, assume there's also no stacktrace
        if (type == Type.OK  &&  message.isEmpty())
            buffer.put((byte)-1);
        else
        {   // Write type, which might be OK, with detail
            buffer.put((byte) type.ordinal());
            PVAString.encodeString(message, buffer);
            PVAString.encodeString(stacktrace, buffer);
        }
    }

    public static PVAStatus decode(final ByteBuffer buffer)
    {
        final byte b = buffer.get();
        if (b == -1)
            return StatusOK;
        if (b < 0  ||  b > 3)
            return new PVAStatus(Type.FATAL, "Invalid status type " + Byte.toUnsignedInt(b), "");
        final Type type = Type.values()[b];
        final String message = PVAString.decodeString(buffer);
        final String stacktrace = PVAString.decodeString(buffer);
        return new PVAStatus(type, message, stacktrace);
    }

    @Override
    public String toString()
    {
        if (type == Type.OK)
            return "OK";
        final StringBuilder buf = new StringBuilder();
        buf.append(type);
        if (! message.isEmpty())
            buf.append(": ").append(message);
        if (! stacktrace.isEmpty())
            buf.append("\n").append(stacktrace);
        return buf.toString();
    }
}
