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

package org.phoebus.security.authorization.tokens;

import org.junit.jupiter.api.Test;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ScopedAuthenticationTokenTest {

    @Test
    public void testScopedAuthenticationToken() {
        ScopedAuthenticationToken scopedAuthenticationToken = new ScopedAuthenticationToken("username", "password");
        assertEquals("username", scopedAuthenticationToken.getUsername());
        assertNull(scopedAuthenticationToken.getScope());

        scopedAuthenticationToken = new ScopedAuthenticationToken("  ", "username", "password");
        assertNull(scopedAuthenticationToken.getScope());

        scopedAuthenticationToken = new ScopedAuthenticationToken("", "username", "password");
        assertNull(scopedAuthenticationToken.getScope());

        scopedAuthenticationToken = new ScopedAuthenticationToken(null, "username", "password");
        assertNull(scopedAuthenticationToken.getScope());

        scopedAuthenticationToken = new ScopedAuthenticationToken("scope", "username", "password");
        assertEquals("scope", scopedAuthenticationToken.getScope());
    }

    @Test
    public void testEqualsAndHashCode() {
        ScopedAuthenticationToken scopedAuthenticationToken1 = new ScopedAuthenticationToken("username", "password");
        ScopedAuthenticationToken scopedAuthenticationToken2 = new ScopedAuthenticationToken("username", "somethingelse");
        ScopedAuthenticationToken scopedAuthenticationToken3 = new ScopedAuthenticationToken("scope", "username", "somethingelse");
        ScopedAuthenticationToken scopedAuthenticationToken4 = new ScopedAuthenticationToken("scope", "username1", "somethingelse");

        assertEquals(scopedAuthenticationToken1, scopedAuthenticationToken2);
        assertNotEquals(scopedAuthenticationToken1, scopedAuthenticationToken3);
        assertNotEquals(scopedAuthenticationToken3, scopedAuthenticationToken4);
        assertEquals(scopedAuthenticationToken1.hashCode(), scopedAuthenticationToken2.hashCode());
        assertNotEquals(scopedAuthenticationToken1.hashCode(), scopedAuthenticationToken3.hashCode());
        assertNotEquals(scopedAuthenticationToken3.hashCode(), scopedAuthenticationToken4.hashCode());

        scopedAuthenticationToken2 = new ScopedAuthenticationToken("username1", "somethingelse");
        assertNotEquals(scopedAuthenticationToken1, scopedAuthenticationToken2);
        assertNotEquals(scopedAuthenticationToken1.hashCode(), scopedAuthenticationToken2.hashCode());

    }
}
