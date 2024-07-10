package org.phoebus.applications.uxanalytics.monitor.backend.authentication;

import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

public class S3AuthenticationProvider implements ServiceAuthenticationProvider {
    @Override
    public void authenticate(String username, String password) {

    }

    @Override
    public void logout(String token) {

    }

    @Override
    public AuthenticationScope getAuthenticationScope() {
        return null;
    }
}
