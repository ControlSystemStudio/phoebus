package org.phoebus.applications.logbook;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogFactory;
import org.phoebus.elog.api.ElogClient.ElogClientBuilder;
import org.phoebus.security.tokens.SimpleAuthenticationToken;

public class ElogLogbook implements LogFactory {

    public static final Logger logger = Logger.getLogger(ElogLogbook.class.getName());
    private static String ID = "elog";
    private LogClient eLogClient;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LogClient getLogClient() {
        if (eLogClient == null) {
            try {
                eLogClient = ElogClientBuilder.serviceURL().create();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create elog client", e);
            }
        }
        return eLogClient;
    }

    @Override
    public LogClient getLogClient(Object authToken) {
        try {
            if (authToken instanceof SimpleAuthenticationToken) {
                SimpleAuthenticationToken token = (SimpleAuthenticationToken) authToken;
                return ElogClientBuilder.serviceURL().username(token.getUsername()).password(token.getPassword()).create();
            } else if (eLogClient == null) {
                eLogClient = ElogClientBuilder.serviceURL().create();

            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create elog client", e);
        }
        return eLogClient;
    }

}
