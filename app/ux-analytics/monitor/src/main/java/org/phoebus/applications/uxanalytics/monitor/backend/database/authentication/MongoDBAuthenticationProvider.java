package org.phoebus.applications.uxanalytics.monitor.backend.database.authentication;

import org.phoebus.applications.uxanalytics.monitor.backend.database.MongoDBConnection;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

public class MongoDBAuthenticationProvider implements ServiceAuthenticationProvider {

    @Override
    public void authenticate(String username, String password) {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        MongoDBConnection.getInstance().connect(connection.getHost(), Integer.parseInt(connection.getPort()), username, password);

    }

    @Override
    public void logout(String token) {

    }

    @Override
    public AuthenticationScope getAuthenticationScope() {
        return AuthenticationScope.MONGODB;
    }
}
