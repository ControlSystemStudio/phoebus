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

package org.phoebus.security.store;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Exercises the SecureStore via a FileBased underlying store implementation.
 */
public class SecureStoreTest {

    private static SecureStore secureStore;

    private static SecureStore memorySecureStore;

    @BeforeAll
    public static void setup() throws Exception {
        File secureStoreFile;
        secureStoreFile = new File(System.getProperty("user.home"), "TestOnlySecureStore.dat");
        if(secureStoreFile.exists()){
            secureStoreFile.delete();
        }
        secureStoreFile.deleteOnExit();

        String password = "forTestPurposesOnly";
        FileBasedStore fileBasedStore = new FileBasedStore(secureStoreFile, password.toCharArray());
        secureStore = new SecureStore(fileBasedStore);

        memorySecureStore = new SecureStore(MemoryBasedStore.getInstance());
    }

    @Test
    public void testGetFromEmpty() throws Exception {
        String value = secureStore.get("some_tag");
        assertNull(value);

        value = memorySecureStore.get("some_tag");
        assertNull(value);
    }

    @Test
    public void testSetAndGet() throws Exception {
        secureStore.set("some_tag", "some_value");
        String value = secureStore.get("some_tag");
        assertEquals(value, "some_value");
        secureStore.delete("some_tag");

        memorySecureStore.set("some_tag", "some_value");
        value = memorySecureStore.get("some_tag");
        assertEquals(value, "some_value");
        memorySecureStore.delete("some_tag");
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

        memorySecureStore.set("some_tag", "some_value");
        value = memorySecureStore.get("some_tag");
        assertEquals(value, "some_value");

        memorySecureStore.set("some_tag", "some_value2");
        value = memorySecureStore.get("some_tag");
        assertEquals(value, "some_value2");

        memorySecureStore.delete("some_tag");
    }

    @Test
    public void testGetNullTag() throws Exception {
        assertNull(secureStore.get(null));
        assertNull(memorySecureStore.get(null));
    }

    @Test
    public void testDeleteNonExisting() throws Exception {
        secureStore.delete("nonExisting");
        memorySecureStore.delete("nonExisting");
    }

    @Test
    public void testGetAllScopedTokens() throws Exception {

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

        memorySecureStore.set(SecureStore.USERNAME_TAG, "username");
        memorySecureStore.set(SecureStore.PASSWORD_TAG, "password");
        memorySecureStore.set("scope1." + SecureStore.USERNAME_TAG, "username1");
        memorySecureStore.set("scope1." + SecureStore.PASSWORD_TAG, "password1");
        memorySecureStore.set("scope2." + SecureStore.USERNAME_TAG, "username2");
        memorySecureStore.set("scope2." + SecureStore.PASSWORD_TAG, "password2");
        memorySecureStore.set("scope3." + SecureStore.USERNAME_TAG, "username3");

        tokens = memorySecureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        secureStore.deleteAllScopedAuthenticationTokens();
    }

    @Test
    public void testGetScopedToken() throws Exception {

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

        memorySecureStore.set(SecureStore.USERNAME_TAG, "username");
        memorySecureStore.set(SecureStore.PASSWORD_TAG, "password");
        memorySecureStore.set("scope1." + SecureStore.USERNAME_TAG, "username1");
        memorySecureStore.set("scope1." + SecureStore.PASSWORD_TAG, "password1");
        memorySecureStore.set("scope2." + SecureStore.USERNAME_TAG, "username2");
        memorySecureStore.set("scope2." + SecureStore.PASSWORD_TAG, "password2");
        memorySecureStore.set("scope3." + SecureStore.USERNAME_TAG, "username3");

        token = memorySecureStore.getScopedAuthenticationToken(null);
        assertNotNull(token);
        assertNull(token.getScope());

        token = memorySecureStore.getScopedAuthenticationToken("scope1");
        assertNotNull(token);
        assertEquals("scope1", token.getScope());
        assertEquals("username1", token.getUsername());

        token = memorySecureStore.getScopedAuthenticationToken("invalid");
        assertNull(token);

        memorySecureStore.deleteAllScopedAuthenticationTokens();
    }


    @Test
    public void testGetAllScopedTokensLegacyOnly() throws Exception {

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
    public void testSetScopedAuthenticationToken() throws Exception {
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope1", "username1", "password1"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope2", "username2", "password2"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope2", "username3", "password3"));

        List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        secureStore.deleteAllScopedAuthenticationTokens();

        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope1", "username1", "password1"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope2", "username2", "password2"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope2", "username3", "password3"));

        tokens = memorySecureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        memorySecureStore.deleteAllScopedAuthenticationTokens();
    }

    @Test
    public void testLegacyAuthenticationToken() throws Exception {
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));

        ScopedAuthenticationToken scopedAuthenticationToken = secureStore.getScopedAuthenticationToken(null);
        assertNotNull(scopedAuthenticationToken);

        secureStore.delete(SecureStore.USERNAME_TAG);
        secureStore.delete(SecureStore.PASSWORD_TAG);
    }

    @Test
    public void testSetInvalidAuthenticationToken() throws Exception {
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
            memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(null, ""));
            fail("Empty password should fail");
        } catch (Exception exception) {
        }

        try {
            memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", null));
            fail("Null user name should fail");
        } catch (Exception exception) {
        }

        try {
            memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(null, "password"));
            fail("Null password should fail");
        } catch (Exception exception) {
        }

        try {
            memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("", null));
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
    public void testDeleteAllScopedAuthenticationTokens() throws Exception {

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

        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope1", "username1", "password1"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope2", "username2", "password2"));

        memorySecureStore.set("somethingElse", "value");

        tokens = memorySecureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        memorySecureStore.deleteAllScopedAuthenticationTokens();
        tokens = memorySecureStore.getAuthenticationTokens();
        assertEquals(0, tokens.size());

        assertNotNull(memorySecureStore.get("somethingElse"));

        memorySecureStore.delete("somethingElse");
    }

    @Test
    public void testOverwriteScopedAuthentication() throws Exception {
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("Scope1", "username1", "password1"));

        ScopedAuthenticationToken token = secureStore.getScopedAuthenticationToken("Scope1");
        assertEquals("username1", token.getUsername());

        secureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope1", "username2", "password1"));
        token = secureStore.getScopedAuthenticationToken("scope1");
        assertEquals("username2", token.getUsername());

        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("Scope1", "username1", "password1"));

        token = memorySecureStore.getScopedAuthenticationToken("Scope1");
        assertEquals("username1", token.getUsername());

        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("scope1", "username2", "password1"));
        token = memorySecureStore.getScopedAuthenticationToken("scope1");
        assertEquals("username2", token.getUsername());
    }
}
