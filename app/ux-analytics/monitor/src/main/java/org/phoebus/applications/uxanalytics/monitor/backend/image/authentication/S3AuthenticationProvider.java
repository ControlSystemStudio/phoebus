package org.phoebus.applications.uxanalytics.monitor.backend.image.authentication;

import org.phoebus.applications.uxanalytics.monitor.backend.image.S3ImageClient;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

public class S3AuthenticationProvider implements ServiceAuthenticationProvider {

    public static final String AUTHENTICATION_SCOPE = "s3";
    /*
       * @param username - access key for S3
       * @param password - secret key for S3
     */
    @Override
    public void authenticate(String username, String password) {
        S3ImageClient.getInstance().connect(username, password);
    }

    @Override
    public void logout(String token) {
        S3ImageClient.getInstance().disconnect();
    }

    @Override
    public AuthenticationScope getAuthenticationScope() {
        return AuthenticationScope.S3;
    }
}
