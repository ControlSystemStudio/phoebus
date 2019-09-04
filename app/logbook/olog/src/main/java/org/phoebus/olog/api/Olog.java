package org.phoebus.olog.api;

import static org.phoebus.olog.api.OlogClient.OlogClientBuilder.*;

import org.phoebus.logbook.LogClient;

public class Olog {
    private static volatile LogClient client;

    private Olog() {

    }

    public static void setClient(LogClient client) {
        Olog.client = client;
    }

    /**
     * Returns the default {@link LogClient}.
     * 
     * @return
     * @throws Exception
     */
    public static LogClient getClient() throws Exception {
        if (client == null) {
            Olog.client = serviceURL().withHTTPAuthentication(false).create();
        }
        return client;
    }

}
