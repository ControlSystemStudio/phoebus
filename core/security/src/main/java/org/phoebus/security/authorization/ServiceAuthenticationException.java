/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.security.authorization;

/**
 * Custom {@link Exception} class to indicate failure to authenticate with a {@link ServiceAuthenticationProvider}.
 * Note that this should <i>not</i> be used to indicate failure to connect to a {@link ServiceAuthenticationProvider}.
 */
public class ServiceAuthenticationException extends RuntimeException{

    public ServiceAuthenticationException(String message){
        super(message);
    }
}


