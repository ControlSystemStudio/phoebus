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

import org.phoebus.applications.saveandrestore.FilterViewApplication;
import org.phoebus.applications.saveandrestore.FilterViewInstance;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.SaveAndRestoreInstance;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.security.store.SecureStoreChangeHandler;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import java.util.List;

public class SecureStoreChangeHandlerImpl implements SecureStoreChangeHandler {

    /**
     * Callback method implementation.
     *
     * @param validTokens A list of valid {@link ScopedAuthenticationToken}s, i.e. a list of tokens associated
     *                    with scopes where is authenticated.
     */
    @Override
    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens) {
        AppDescriptor saveAndRestoreAppDescriptor = ApplicationService.findApplication(SaveAndRestoreApplication.NAME);
        if (saveAndRestoreAppDescriptor instanceof SaveAndRestoreApplication saveAndRestoreApplication) {
            SaveAndRestoreInstance saveAndRestoreInstance = saveAndRestoreApplication.getInstance();
            // Save&restore app may not be launched (yet)
            if(saveAndRestoreInstance == null){
                return;
            }
            saveAndRestoreInstance.secureStoreChanged(validTokens);
        }

        AppDescriptor filterViewAppDescriptor = ApplicationService.findApplication(FilterViewApplication.NAME);
        if (filterViewAppDescriptor instanceof FilterViewApplication filterViewApplication) {
            FilterViewInstance filterViewInstance = filterViewApplication.getInstance();
            // Save&restore filter view app may not be launched (yet)
            if(filterViewInstance == null){
                return;
            }
            filterViewInstance.secureStoreChanged(validTokens);
        }
    }
}
