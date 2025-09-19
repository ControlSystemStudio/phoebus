/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.security.store;

import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

import java.net.ConnectException;

public class ServiceAuthenticationProviderOne implements ServiceAuthenticationProvider {

    @Override
    public void authenticate(String username, String password) throws ConnectException {

    }

    @Override
    public AuthenticationScope getAuthenticationScope() {
        return new AuthenticationScope() {
            @Override
            public String getScope() {
                return "service1";
            }

            @Override
            public String getDisplayName() {
                return "";
            }
        };
    }
}
