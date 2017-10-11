/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.exceptions;

/**
 * Signals that the method is failing due to a misconfiguration of the
 * library which should be fixed by the user of such library.
 *
 * @author carcassi
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Creates a new configuration exception.
     */
    public ConfigurationException() {
    }

    /**
     * Creates a new configuration exception with the given message.
     *
     * @param message a message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
