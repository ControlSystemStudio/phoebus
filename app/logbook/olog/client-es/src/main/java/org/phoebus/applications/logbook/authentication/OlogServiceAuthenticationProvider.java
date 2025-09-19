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
import org.phoebus.security.authorization.ServiceAuthenticationException;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

import java.net.ConnectException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OlogServiceAuthenticationProvider implements ServiceAuthenticationProvider {

    private final AuthenticationScope ologAuthenticationScope;

    public OlogServiceAuthenticationProvider(){
        ologAuthenticationScope = new OlogAuthenticationScope();
    }

    @Override
    public void authenticate(String username, String password) throws ConnectException{
        try {
            OlogHttpClient.builder().build().authenticate(username, password);
        }
        catch(ConnectException e){
            throw e;
        }
        catch (Exception e) {
            Logger.getLogger(OlogServiceAuthenticationProvider.class.getName())
                    .log(Level.WARNING, "Failed to authenticate user " + username + " with logbook service", e);
            throw new ServiceAuthenticationException("Failed to authenticate user " + username + " with logbook service");
        }
    }

    @Override
    public AuthenticationScope getAuthenticationScope() {
        return ologAuthenticationScope;
    }
}
