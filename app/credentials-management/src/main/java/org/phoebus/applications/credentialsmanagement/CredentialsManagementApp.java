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

package org.phoebus.applications.credentialsmanagement;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.store.SecureStore;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.net.URL;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

/**
 * Simple app that launches an UI offering logout capabilities for scopes/applications that use and maintain
 * credentials, e.g.logbook. This is not in any way associated with the APIs controlling Phoebus internal
 * authorizations for the various apps. The credentials in the case of the Credentials Management app are
 * typically associated with credentials used to authenticate against external services.
 */
public class CredentialsManagementApp implements AppDescriptor {

    public static final String name = "credentials_management";

    public static final String DisplayName = Messages.DisplayName;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URL getIconURL() {
        return CredentialsManagementApp.class.getResource("/icons/credentials.png");
    }

    @Override
    public String getDisplayName() {
        return DisplayName;
    }

    @Override
    public AppInstance create() {
        List<ServiceAuthenticationProvider> authenticationProviders =
                ServiceLoader.load(ServiceAuthenticationProvider.class).stream().map(Provider::get).collect(Collectors.toList());
        try {
            SecureStore secureStore = new SecureStore();
            new CredentialsManagementStage(authenticationProviders, secureStore).show();
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(Messages.SecureStoreErrorTitle, Messages.SecureStoreErrorBody, e);
        }
        return null;
    }
}
