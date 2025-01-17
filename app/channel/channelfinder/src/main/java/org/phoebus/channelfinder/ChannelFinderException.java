/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

/**
 * A Exception Type for various channelfinder exception conditions.
 *
 * @author shroffk
 */
public class ChannelFinderException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 6279865221993808192L;

    private int httpStatus;

    public ChannelFinderException(String message) {
        super(message);
    }

    public ChannelFinderException(Throwable t) {
        super(t);
    }

    public ChannelFinderException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * Returns the associated HTTP status code which caused this exception.
     *
     * @return the status
     */
    public int getStatus() {
        return httpStatus;
    }

}
