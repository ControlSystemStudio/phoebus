/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
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
    /** "OK" status */
    public static final PVAStatus StatusOK = new PVAStatus(Type.OK, "", "");

    /** Type or severity of status message */
    public enum Type
    {
        /** OK (info) */
        OK,
        /** Warning */
        WARNING,
        /** Error */
        ERROR,
        /** Fatal */
        FATAL;
    }

    /** Type */
    public final Type type;
    /** Message */
    public final String message;
    /** Stack trace */
    public final String stacktrace;

    /** @param type Type
     *  @param message Message
     *  @param stacktrace Stack trace
     */
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

    /** @param buffer Target buffer */
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

    /** @param buffer Source buffer
     *  @return Decoded status
     */
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
