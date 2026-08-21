package org.epics.pva.exceptions;

/** Protocol violation while decoding a PVA message. */
@SuppressWarnings("nls")
public class PVAProtocolException extends Exception
{
    public PVAProtocolException(final String message)
    {
        super(message);
    }
}
