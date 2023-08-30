package org.phoebus.applications.logbook;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogFactory;
import org.phoebus.olog.api.OlogClient.OlogClientBuilder;
import org.phoebus.security.tokens.SimpleAuthenticationToken;

public class OlogLogbook implements LogFactory {

    public static final Logger logger = Logger.getLogger(OlogLogbook.class.getName());
    private static final String ID = "olog";
    private LogClient oLogClient;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LogClient getLogClient() {
        if (oLogClient == null) {
            try {
                oLogClient = OlogClientBuilder.serviceURL().create();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create olog client", e);
            }
        }
        return oLogClient;
    }

    @Override
    public LogClient getLogClient(Object authToken) {
        try {
            if (authToken instanceof SimpleAuthenticationToken) {
                SimpleAuthenticationToken token = (SimpleAuthenticationToken) authToken;
                return OlogClientBuilder.serviceURL().withHTTPAuthentication(true).username(token.getUsername()).password(token.getPassword())
                        .create();
            } else if (oLogClient == null) {
                oLogClient = OlogClientBuilder.serviceURL().create();

            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create olog client", e);
        }
        return oLogClient;
    }

}
