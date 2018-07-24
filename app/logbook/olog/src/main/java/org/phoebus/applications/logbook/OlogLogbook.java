package org.phoebus.applications.logbook;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogFactory;
import org.phoebus.olog.api.OlogClient;
import org.phoebus.olog.api.OlogClient.OlogClientBuilder;

public class OlogLogbook implements LogFactory {

    private static final Logger logger = Logger.getLogger(OlogLogbook.class.getName());
    private static final String ID = "olog";
    private OlogClient oLogClient;

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
        if (oLogClient == null) {
            try {
                oLogClient = OlogClientBuilder.serviceURL().create();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create olog client", e);
            }
        }
        return oLogClient;
    }

}
