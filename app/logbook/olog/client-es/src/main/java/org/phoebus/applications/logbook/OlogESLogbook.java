package org.phoebus.applications.logbook;

import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogFactory;
import org.phoebus.olog.es.api.OlogClient.OlogClientBuilder;
import org.phoebus.security.tokens.SimpleAuthenticationToken;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logbook client for the new es based olog.
 * TODO: in the future this client would replace the old olog client
 *
 * @author kunal
 */
public class OlogESLogbook implements LogFactory {

    private static final Logger logger = Logger.getLogger(OlogESLogbook.class.getName());
    private static final String ID = "olog-es";

    @Override
    public String getId() {
        return ID;
    }

    /**
     *
     * @return A fresh instance of the client. Instead of maintaining a client reference in this class,
     * a new instance is return since the user may have signed out or signed in thus invalidating
     * the authentication token.
     */
    @Override
    public LogClient getLogClient() {
        try {
            return OlogClientBuilder.serviceURL().create();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create olog es client", e);
        }
        return null;
    }

    /**
     * @param authToken An authentication token.
     * @return A fresh instance of the client. Instead of maintaining a client reference in this class,
     * a new instance to force usage of the specified authentication token.
     */
    @Override
    public LogClient getLogClient(Object authToken) {
        try {
            if (authToken instanceof SimpleAuthenticationToken) {
                SimpleAuthenticationToken token = (SimpleAuthenticationToken) authToken;
                return OlogClientBuilder.serviceURL().withHTTPAuthentication(true).username(token.getUsername()).password(token.getPassword())
                        .create();
            } else {
                return getLogClient();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create olog client", e);
        }
        return null;
    }
}
