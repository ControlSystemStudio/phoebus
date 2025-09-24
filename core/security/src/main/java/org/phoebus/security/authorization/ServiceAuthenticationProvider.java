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

package org.phoebus.security.authorization;

import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;

/**
 * Implementations of this interface are used to announce support for
 * a log in procedure to a service.
 */
public interface ServiceAuthenticationProvider {

    /**
     * Authenticates with the announced service.
     * @param username User name
     * @param password Password
     * @return An {@link AuthenticationStatus} indicating the outcome.
     */
    AuthenticationStatus authenticate(String username, String password);

    /**
     * Signs out user from the service.
     */
    default void logout(){
        try {
            SecureStore secureStore = new SecureStore();
            secureStore.deleteScopedAuthenticationToken(getAuthenticationScope());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The identity of the announced service. This must be unique between all implementations.
     * <b>NOTE:</b> This identity is used to create keys (aka aliases)
     * under which credentials are persisted in the
     * {@link org.phoebus.security.store.SecureStore}. Such keys are stored in
     * <b>lower</b> case in the key store that backs {@link org.phoebus.security.store.SecureStore}, and
     * is a behavior defined by the encryption scheme implementation.
     * Consequently, an identity like "UPPER" will be persisted as "upper", i.e. case insensitivity
     * must be considered when defining an identity.
     * @return Service name
     */
    AuthenticationScope getAuthenticationScope();

}
