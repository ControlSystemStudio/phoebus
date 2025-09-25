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

package org.phoebus.security.tokens;

import java.util.Objects;

/**
 * Extension of  {@link SimpleAuthenticationToken}.
 *
 * Note that the unique identity of a {@link ScopedAuthenticationToken} instance as defined in the
 * <code>scope</code> field is maintained as a lower case string, effectively rendering the identity as
 * case-insensitive. Reason is that if a Java native secure store file is used to maintain the tokens,
 * the identity key is saved in lower case in the secure store.
 */
public class ScopedAuthenticationToken extends SimpleAuthenticationToken{

    private AuthenticationScope scope;

    /** @param username Username
     *  @param password Password
     */
    public ScopedAuthenticationToken(String username, String password){
        super(username, password);
    }

    /** @param scope Scope identity, will be converted to lower case.
     *  @param username Username
     *  @param password Password
     */
    public ScopedAuthenticationToken(AuthenticationScope scope, String username, String password){
        this(username, password);
        if(scope != null){
            if(scope.getScope().trim().isEmpty()){
                this.scope = null;
            }
            else{
                this.scope = scope;
            }
        }
    }

    /** @return Scope */
    public AuthenticationScope getAuthenticationScope(){
        return scope;
    }

    @Override
    public boolean equals(Object other){
        if(!(other instanceof ScopedAuthenticationToken)){
            return false;
        }
        ScopedAuthenticationToken otherToken = (ScopedAuthenticationToken)other;
        return (otherToken.getAuthenticationScope() + "." + otherToken.getUsername()).equals(getAuthenticationScope() + "." + getUsername());
    }

    @Override
    public int hashCode(){
        return Objects.hash(getAuthenticationScope(), getUsername());
    }

    @Override
    public String toString(){
        return "Scope: " + (scope != null ? scope.getScope() : "") + ", username: " + getUsername();
    }
}
