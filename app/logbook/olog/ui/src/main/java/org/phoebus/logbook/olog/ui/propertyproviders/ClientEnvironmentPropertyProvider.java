/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.logbook.olog.ui.propertyproviders;

import org.phoebus.logbook.Property;
import org.phoebus.logbook.olog.ui.write.LogPropertyProvider;
import org.phoebus.olog.es.api.model.OlogProperty;
import org.phoebus.ui.application.Messages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientEnvironmentPropertyProvider implements LogPropertyProvider {

    @Override
    public Property getProperty() {
        String hostname = "N/A";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Logger.getLogger(ClientEnvironmentPropertyProvider.class.getName())
                    .log(Level.INFO, "Unable to determine hostname", e);
        }
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Hostname", hostname);
        attributes.put("Client", "Phoebus " + Messages.AppVersion);
        Property property = new OlogProperty("Client Environment", attributes);
        return property;
    }
}
