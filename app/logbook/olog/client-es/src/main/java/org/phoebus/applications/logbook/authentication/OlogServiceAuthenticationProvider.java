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

package org.phoebus.applications.logbook.authentication;

import org.phoebus.olog.es.api.OlogHttpClient;
import org.phoebus.security.authorization.AuthenticationStatus;
import org.phoebus.security.authorization.ServiceAuthenticationException;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

import java.net.ConnectException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OlogServiceAuthenticationProvider implements ServiceAuthenticationProvider {

    private final AuthenticationScope ologAuthenticationScope;

    private final Logger logger = Logger.getLogger(OlogServiceAuthenticationProvider.class.getName());

    public OlogServiceAuthenticationProvider() {
        ologAuthenticationScope = new OlogAuthenticationScope();
    }

    @Override
    public AuthenticationStatus authenticate(String username, String password) {
        try {
            OlogHttpClient.builder().build().authenticate(username, password);
            return AuthenticationStatus.AUTHENTICATED;
        } catch (ConnectException e) {
            logger.log(Level.WARNING, "Unable to connect to logbook service");
            return AuthenticationStatus.SERVICE_OFFLINE;
        } catch (ServiceAuthenticationException e){
            logger.log(Level.WARNING, "User " + username + " not authenticated");
            return AuthenticationStatus.BAD_CREDENTIALS;
        }
        catch (Exception e) {
            // NOTE!!! Exception message and/or stack trace could contain request URL and consequently
            // user's password, so do not log or propagate it.
            logger.log(Level.WARNING, "Failed to authenticate user " + username + " with logbook service, reason unknown");
            return AuthenticationStatus.UNKNOWN_ERROR;
        }
    }

    @Override
    public AuthenticationScope getAuthenticationScope() {
        return ologAuthenticationScope;
    }
}
