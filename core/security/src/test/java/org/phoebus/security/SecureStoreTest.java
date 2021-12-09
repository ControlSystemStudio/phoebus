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

package org.phoebus.security;

import org.junit.BeforeClass;
import org.junit.Test;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SecureStoreTest {

    private static SecureStore secureStore;

    @BeforeClass
    public static void init() throws Exception {
        File secureStoreFile;
        secureStoreFile = new File(System.getProperty("user.home"), "TestOnlySecureStore.dat");
        secureStoreFile.deleteOnExit();

        String password = "forTestPurposesOnly";
        secureStore = new SecureStore(secureStoreFile, password.toCharArray());
    }

    @Test
    public void testGetFromEmpty() throws Exception {
        String value = secureStore.get("some_tag");
        assertNull(value);
    }

    @Test
    public void testSetAndGet() throws Exception {
        secureStore.set("some_tag", "some_value");
        String value = secureStore.get("some_tag");
        assertEquals(value, "some_value");
        secureStore.delete("some_tag");
    }

    @Test
    public void testSetDuplicateAndGet() throws Exception {
        secureStore.set("some_tag", "some_value");
        String value = secureStore.get("some_tag");
        assertEquals(value, "some_value");

        secureStore.set("some_tag", "some_value2");
        value = secureStore.get("some_tag");
        assertEquals(value, "some_value2");

        secureStore.delete("some_tag");
    }

    @Test(expected = NullPointerException.class)
    public void testGetNullTag() throws Exception {
        secureStore.get(null);
    }

    @Test
    public void testDeleteNonExisting() throws Exception{
        secureStore.delete("nonExisting");
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteNull() throws Exception{
        secureStore.delete(null);
    }

    @Test
    public void testGetAllScopedTokens() throws  Exception{

        secureStore.set(SecureStore.USERNAME_TAG, "username");
        secureStore.set(SecureStore.PASSWORD_TAG, "password");
        secureStore.set("scope1." + SecureStore.USERNAME_TAG, "username1");
        secureStore.set("scope1." + SecureStore.PASSWORD_TAG, "password1");
        secureStore.set("scope2." + SecureStore.USERNAME_TAG, "username2");
        secureStore.set("scope2." + SecureStore.PASSWORD_TAG, "password2");
        secureStore.set("scope3." + SecureStore.USERNAME_TAG, "username3");

        List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());
        assertEquals("username", tokens.get(0).getUsername());
        assertEquals("password", tokens.get(0).getPassword());

        secureStore.deleteAllScopedAuthenticationTokens();
    }

    @Test
    public void testGetAllScopedToken() throws  Exception{

        secureStore.set(SecureStore.USERNAME_TAG, "username");
        secureStore.set(SecureStore.PASSWORD_TAG, "password");
        secureStore.set("scope1." + SecureStore.USERNAME_TAG, "username1");
        secureStore.set("scope1." + SecureStore.PASSWORD_TAG, "password1");
        secureStore.set("scope2." + SecureStore.USERNAME_TAG, "username2");
        secureStore.set("scope2." + SecureStore.PASSWORD_TAG, "password2");
        secureStore.set("scope3." + SecureStore.USERNAME_TAG, "username3");

        ScopedAuthenticationToken token = secureStore.getScopedAuthenticationToken(null);
        assertNotNull(token);
        assertNull(token.getScope());

        token = secureStore.getScopedAuthenticationToken("scope1");
        assertNotNull(token);
        assertEquals("scope1", token.getScope());
        assertEquals("username1", token.getUsername());

        token = secureStore.getScopedAuthenticationToken("invalid");
        assertNull(token);

        secureStore.deleteAllScopedAuthenticationTokens();
    }


    @Test
    public void testGetAllScopedTokensLegacyOnly() throws  Exception{

        List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
        assertEquals(0, tokens.size());

        secureStore.set(SecureStore.USERNAME_TAG, "username");
        secureStore.set(SecureStore.PASSWORD_TAG, "password");

        tokens = secureStore.getAuthenticationTokens();
        assertEquals(1, tokens.size());
        assertEquals("username", tokens.get(0).getUsername());
        assertEquals("password", tokens.get(0).getPassword());

        secureStore.delete(SecureStore.USERNAME_TAG);
        secureStore.delete(SecureStore.PASSWORD_TAG);
    }

    @Test
    public void testSetScopedAuthenticationToken() throws Exception{
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope1", "username1", "password1"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope2", "username2", "password2"));

        List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());
        assertEquals("username", tokens.get(0).getUsername());
        assertEquals("password", tokens.get(0).getPassword());
        assertNull(tokens.get(0).getScope());
        assertEquals("username1", tokens.get(1).getUsername());
        assertEquals("password1", tokens.get(1).getPassword());
        assertEquals("scope1", tokens.get(1).getScope());

        secureStore.deleteAllScopedAuthenticationTokens();
    }

    @Test
    public void testLegacyAuthenticationToken() throws Exception{
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));

        ScopedAuthenticationToken scopedAuthenticationToken = secureStore.getScopedAuthenticationToken(null);
        assertNotNull(scopedAuthenticationToken);

        secureStore.delete(SecureStore.USERNAME_TAG);
        secureStore.delete(SecureStore.PASSWORD_TAG);
    }

    @Test
    public void testSetInvalidAuthenticationToken() throws Exception{
        try {
            secureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", null));
            fail("Null user name should fail");
        } catch (Exception exception) {
        }

        try {
            secureStore.setScopedAuthentication(new ScopedAuthenticationToken(null, "password"));
            fail("Null password should fail");
        } catch (Exception exception) {
        }

        try {
            secureStore.setScopedAuthentication(new ScopedAuthenticationToken("", null));
            fail("Empty user name should fail");
        } catch (Exception exception) {
        }

        try {
            secureStore.setScopedAuthentication(new ScopedAuthenticationToken(null, ""));
            fail("Empty password should fail");
        } catch (Exception exception) {
        }
    }

    @Test
    public void testDeleteAllScopedAuthenticationTokens() throws Exception{

        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope1", "username1", "password1"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope2", "username2", "password2"));

        secureStore.set("somethingElse", "value");

        List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        secureStore.deleteAllScopedAuthenticationTokens();
        tokens = secureStore.getAuthenticationTokens();
        assertEquals(0, tokens.size());

        assertNotNull(secureStore.get("somethingElse"));

        secureStore.delete("somethingElse");
    }

    @Test
    public void testOverwriteScopedAuthentication() throws Exception{
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("Scope1", "username1", "password1"));

        ScopedAuthenticationToken token = secureStore.getScopedAuthenticationToken("Scope1");
        assertEquals("username1", token.getUsername());

        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope1", "username2", "password1"));
        token = secureStore.getScopedAuthenticationToken("scope1");
        assertEquals("username2", token.getUsername());
    }
}
