package org.phoebus.applications.logbook;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogFactory;
import org.phoebus.olog.es.api.OlogClient;
import org.phoebus.olog.es.api.OlogClient.OlogClientBuilder;
/**
 * Logbook client for the new es based olog.
 * TODO: in the future this client would replace the old olog client
 * @author kunal
 *
 */
public class OlogESLogbook implements LogFactory {

    private static final Logger logger = Logger.getLogger(OlogESLogbook.class.getName());
    private static final String ID = "olog-es";
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
                logger.log(Level.SEVERE, "Failed to create olog es client", e);
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
                logger.log(Level.SEVERE, "Failed to create olog es client", e);
            }
        }
        return oLogClient;
    }

}
