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
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import java.io.File;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the SecureStore via a FileBased underlying store implementation.
 */
public class SecureStoreTest {

    private static SecureStore secureStore;

    private static SecureStore memorySecureStore;

    private static List<ServiceAuthenticationProvider> authenticationProviders;

    @BeforeAll
    public static void setup() throws Exception {
        authenticationProviders =
                ServiceLoader.load(ServiceAuthenticationProvider.class).stream().map(ServiceLoader.Provider::get)
                        .collect(Collectors.toList());
        File secureStoreFile;
        secureStoreFile = new File(System.getProperty("user.home"), "TestOnlySecureStore.dat");
        if (secureStoreFile.exists()) {
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
        secureStore.set("service1." + SecureStore.USERNAME_TAG, "username1");
        secureStore.set("service1." + SecureStore.PASSWORD_TAG, "password1");
        secureStore.set("service2." + SecureStore.USERNAME_TAG, "username2");
        secureStore.set("service2." + SecureStore.PASSWORD_TAG, "password2");

        List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());
        assertEquals("username", tokens.get(0).getUsername());
        assertEquals("password", tokens.get(0).getPassword());

        secureStore.deleteAllScopedAuthenticationTokens();

        memorySecureStore.set(SecureStore.USERNAME_TAG, "username");
        memorySecureStore.set(SecureStore.PASSWORD_TAG, "password");
        memorySecureStore.set("service1." + SecureStore.USERNAME_TAG, "username1");
        memorySecureStore.set("service1." + SecureStore.PASSWORD_TAG, "password1");
        memorySecureStore.set("service2." + SecureStore.USERNAME_TAG, "username2");
        memorySecureStore.set("service2." + SecureStore.PASSWORD_TAG, "password2");

        tokens = memorySecureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        secureStore.deleteAllScopedAuthenticationTokens();
    }

    @Test
    public void testGetScopedToken() throws Exception {

        secureStore.set(SecureStore.USERNAME_TAG, "username");
        secureStore.set(SecureStore.PASSWORD_TAG, "password");
        secureStore.set("service1." +SecureStore.USERNAME_TAG, "username1");
        secureStore.set("service1." + SecureStore.PASSWORD_TAG, "password1");
        secureStore.set("service2." + SecureStore.USERNAME_TAG, "username2");
        secureStore.set("service2." + SecureStore.PASSWORD_TAG, "password2");

        ScopedAuthenticationToken token = secureStore.getScopedAuthenticationToken(null);
        assertNotNull(token);
        assertNull(token.getAuthenticationScope());

        token = secureStore.getScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope());
        assertNotNull(token);
        assertEquals("service1", token.getAuthenticationScope().getScope());
        assertEquals("username1", token.getUsername());

        token = secureStore.getScopedAuthenticationToken(null);
        assertNotNull(token);

        secureStore.deleteAllScopedAuthenticationTokens();

        memorySecureStore.set(SecureStore.USERNAME_TAG, "username");
        memorySecureStore.set(SecureStore.PASSWORD_TAG, "password");
        memorySecureStore.set("service1." + SecureStore.USERNAME_TAG, "username1");
        memorySecureStore.set("service1." + SecureStore.PASSWORD_TAG, "password1");
        memorySecureStore.set("service2." + SecureStore.USERNAME_TAG, "username2");
        memorySecureStore.set("service2." + SecureStore.PASSWORD_TAG, "password2");

        token = memorySecureStore.getScopedAuthenticationToken(null);
        assertNotNull(token);
        assertNull(token.getAuthenticationScope());

        token = memorySecureStore.getScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope());
        assertNotNull(token);
        assertEquals("service1", token.getAuthenticationScope().getScope());
        assertEquals("username1", token.getUsername());

        token = memorySecureStore.getScopedAuthenticationToken(null);
        assertNotNull(token);

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
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope(), "username1", "password1"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(1).getAuthenticationScope(), "username2", "password2"));

        List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        secureStore.deleteAllScopedAuthenticationTokens();

        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope(), "username1", "password1"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(1).getAuthenticationScope(), "username2", "password2"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(1).getAuthenticationScope(), "username3", "password3"));

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
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope(), "username1", "password1"));
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(1).getAuthenticationScope(), "username2", "password2"));

        List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        secureStore.deleteAllScopedAuthenticationTokens();
        tokens = secureStore.getAuthenticationTokens();
        assertEquals(0, tokens.size());

        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken("username", "password"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope(), "username1", "password1"));
        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(1).getAuthenticationScope(), "username2", "password2"));

        tokens = memorySecureStore.getAuthenticationTokens();
        assertEquals(3, tokens.size());

        memorySecureStore.deleteAllScopedAuthenticationTokens();
        tokens = memorySecureStore.getAuthenticationTokens();
        assertEquals(0, tokens.size());
    }

    @Test
    public void testOverwriteScopedAuthentication() throws Exception {
        secureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope(), "username1", "password1"));

        ScopedAuthenticationToken token = secureStore.getScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope());
        assertEquals("username1", token.getUsername());

        secureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope(), "username2", "password1"));
        token = secureStore.getScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope());
        assertEquals("username2", token.getUsername());

        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope(), "username1", "password1"));

        token = memorySecureStore.getScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope());
        assertEquals("username1", token.getUsername());

        memorySecureStore.setScopedAuthentication(new ScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope(), "username2", "password1"));
        token = memorySecureStore.getScopedAuthenticationToken(authenticationProviders.get(0).getAuthenticationScope());
        assertEquals("username2", token.getUsername());
    }
}
