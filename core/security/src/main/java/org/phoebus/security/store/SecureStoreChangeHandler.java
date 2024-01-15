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

package org.phoebus.security.store;

import org.phoebus.security.tokens.ScopedAuthenticationToken;

import java.util.List;

/**
 * Interface used to listen to changes in authentication status. Implementations can register over SPI
 * to get notified when user logs in or logs out from an {@link org.phoebus.security.tokens.AuthenticationScope}.
 */
public interface SecureStoreChangeHandler {

    /**
     * Callback method implemented by listeners.
     * @param validTokens A list of valid {@link ScopedAuthenticationToken}s, i.e. a list of tokens associated
     *                    with scopes where is authenticated.
     */
    void secureStoreChanged(List<ScopedAuthenticationToken> validTokens);
}
