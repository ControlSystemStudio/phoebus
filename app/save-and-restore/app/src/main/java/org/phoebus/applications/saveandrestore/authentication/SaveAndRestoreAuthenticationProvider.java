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
 *
 */

package org.phoebus.applications.saveandrestore.authentication;

import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication provider for the save-and-restore service.
 */
public class SaveAndRestoreAuthenticationProvider implements ServiceAuthenticationProvider {

    @Override
    public void authenticate(String username, String password){
        SaveAndRestoreService saveAndRestoreService = SaveAndRestoreService.getInstance();
        try {
            saveAndRestoreService.authenticate(username, password);
        } catch (Exception e) {
            Logger.getLogger(SaveAndRestoreAuthenticationProvider.class.getName())
                    .log(Level.WARNING, "Failed to authenticate user " + username + " against save&restore service", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void logout(String token) {
        // Not implemented for save&restore
    }

    @Override
    public AuthenticationScope getAuthenticationScope(){
        return AuthenticationScope.SAVE_AND_RESTORE;
    }

}
