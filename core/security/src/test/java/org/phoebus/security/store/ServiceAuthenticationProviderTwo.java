/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.security.store;

import org.phoebus.security.authorization.AuthenticationStatus;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

public class ServiceAuthenticationProviderTwo implements ServiceAuthenticationProvider {

    @Override
    public AuthenticationStatus authenticate(String username, String password) {
        return AuthenticationStatus.AUTHENTICATED;
    }

    @Override
    public AuthenticationScope getAuthenticationScope() {
        return new AuthenticationScope() {
            @Override
            public String getScope() {
                return "service2";
            }

            @Override
            public String getDisplayName() {
                return "";
            }
        };
    }
}
