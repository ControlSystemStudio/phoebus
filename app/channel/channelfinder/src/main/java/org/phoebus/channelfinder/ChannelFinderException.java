/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.SocketException;

import javax.swing.text.html.parser.ParserDelegator;

/**
 * A Exception Type for various channelfinder exception conditions.
 * 
 * @author shroffk
 *
 */
public class ChannelFinderException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 6279865221993808192L;

    private Status status;

    public ChannelFinderException() {
        super();
    }

    public ChannelFinderException(String message) {
        super(message);
    }

    public ChannelFinderException(UniformInterfaceException cause) {
        super(parseErrorMsg(cause), cause);
        this.setStatus(Status.fromStatusCode(cause.getResponse().getStatus()));
    }

    public ChannelFinderException(ClientHandlerException cause) {
        super(cause.getMessage());
        if(cause.getCause() instanceof SocketException) {
            this.setStatus(Status.NOT_FOUND);
        } else {
            this.setStatus(Status.SEE_OTHER);
        }
    }

    private static String parseErrorMsg(UniformInterfaceException ex) {
        String entity = ex.getResponse().getEntity(String.class);
        try {
            ClientResponseParser callback = new ClientResponseParser();
            Reader reader = new StringReader(entity);
            new ParserDelegator().parse(reader, callback, false);
            return callback.getMessage();
        } catch (IOException e) {
            return "Could not retrieve message from server";
        }
    }

    public ChannelFinderException(Status status, String message) {
        super(message);
        this.setStatus(status);
    }

    /**
     * 
     * @param status
     *            - the http error status code
     * @param cause
     *            - the original UniformInterfaceException
     * @param message
     *            - additional error information
     */
    public ChannelFinderException(Status status, Throwable cause, String message) {
        super(message, cause);
        this.setStatus(status);
    }

    /**
     * Set the associated HTTP status code which caused this exception.
     * 
     * @param status
     *            the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns the associated HTTP status code which caused this exception.
     * 
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

}
