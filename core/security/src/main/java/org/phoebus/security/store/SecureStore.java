/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.security.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.phoebus.framework.workbench.Locations;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

/** Secure Store
 *
 *  <p>Writes tag/value pairs into an encrypted file.
 *
 *  <p>Does depend on a password to read and write the file.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SecureStore
{
    private final SecretKeyFactory kf = SecretKeyFactory.getInstance("PBE");
    private final KeyStore store;
    private final File secure_file;
    private final char[] store_pass;
    private final ProtectionParameter pp;

    public static final String USERNAME_TAG = "username";
    public static final String PASSWORD_TAG = "password";

    private static final Logger LOGGER = Logger.getLogger(SecureStore.class.getName());

    /** Create with default file in 'user' location */
    public SecureStore() throws Exception
    {
        this(new File(Locations.user(), "secure_store.dat"));
    }

    /** Create with default password.
     *
     *  <p>Knowledge of this code would allow reading the file
     *
     *  @param secure_file File to read or write
     *  @throws Exception on error
     */
    public SecureStore(final File secure_file) throws Exception
    {
        this(secure_file, Integer.toString(secure_file.getAbsolutePath().hashCode()).toCharArray());
    }

    /** Create
     *  @param secure_file File to read or write
     *  @param store_pass Password for encoding/decoding entries
     *  @throws Exception on error
     */
    public SecureStore(final File secure_file, final char[] store_pass) throws Exception
    {
        this.secure_file = secure_file;
        this.store_pass = store_pass;

        store = KeyStore.getInstance(KeyStore.getDefaultType());

        pp = new KeyStore.PasswordProtection(store_pass);


        // Load existing file or initialize as empty
        if (secure_file.canRead())
            store.load(new FileInputStream(secure_file), store_pass);
        else
            store.load(null, store_pass);
    }

    /** Read an entry from the store
     *  @param tag Tag that identifies the entry
     *  @return Stored text or <code>null</code>
     *  @throws Exception on error
     */
    public String get(final String tag) throws Exception
    {
        final KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) store.getEntry(tag, pp);
        if (entry == null)
            return null;

        final PBEKeySpec key = (PBEKeySpec) kf.getKeySpec(entry.getSecretKey(), PBEKeySpec.class);
        return new String(key.getPassword());
    }

    /** Write an entry to the store. If the entry already exists, it will be overwritten.
     *  @param tag Tag that identifies the entry
     *  @param value Value of the entry
     *  @throws Exception on error
     */
    public void set(final String tag, final String value) throws Exception
    {
        final SecretKey skey = kf.generateSecret(new PBEKeySpec(value.toCharArray()));
        store.setEntry(tag, new KeyStore.SecretKeyEntry(skey), pp);

        // Write file whenever an entry is changed
        store.store(new FileOutputStream(secure_file), store_pass);
    }

    /**
     * Deletes an entry in the secure store.
     * @param tag The tag to delete, must not be <code>null</code>.
     * @throws Exception
     */
    public void delete(String tag) throws Exception{
        store.deleteEntry(tag);
        LOGGER.log(Level.INFO, "Deleting entry " + tag + " from secure store");
        // Write file whenever an entry is changed
        store.store(new FileOutputStream(secure_file), store_pass);
    }

    public ScopedAuthenticationToken getScopedAuthenticationToken(String scope) throws Exception{
        String username;
        String password;
        if(scope == null || scope.trim().isEmpty()){
            username = get(USERNAME_TAG);
            password = get(PASSWORD_TAG);
        }
        else{
            username = get(scope + "." + USERNAME_TAG);
            password = get(scope + "." + PASSWORD_TAG);
        }
        if(username == null || password == null){
            return null;
        }
        return new ScopedAuthenticationToken(scope, username, password);
    }

    public void deleteScopedAuthenticationToken(String scope) throws Exception{
        LOGGER.log(Level.INFO, "Deleting authentication token for scope: " + scope);
        if(scope == null || scope.trim().isEmpty()){
            delete(USERNAME_TAG);
            delete(PASSWORD_TAG);
        }
        else{
            delete(scope + "." + USERNAME_TAG);
            delete(scope + "." + PASSWORD_TAG);
        }
    }

    public void deleteAllScopedAuthenticationTokens() throws Exception{
        List<ScopedAuthenticationToken> allScopedAuthenticationTokens = getAuthenticationTokens();
        allScopedAuthenticationTokens.stream().forEach(token -> {
            try {
                deleteScopedAuthenticationToken(token.getScope());
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to delete scoped authentication token " + token.toString(), exception);
            }
        });
    }

    public void setScopedAuthentication(ScopedAuthenticationToken scopedAuthenticationToken) throws Exception{
        String username = scopedAuthenticationToken.getUsername();
        String password = scopedAuthenticationToken.getPassword();
        if(username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()){
            throw new RuntimeException("Username and password must both be non-null and non-empty");
        }
        String scope = scopedAuthenticationToken.getScope();
        if(scope == null || scope.trim().isEmpty()){
            set(USERNAME_TAG, username);
            set(PASSWORD_TAG, password);
        }
        else{
            set(scope + "." + USERNAME_TAG, username);
            set(scope + "." + PASSWORD_TAG, password);
        }
        LOGGER.log(Level.INFO, "Storing scoped authentication token " + scopedAuthenticationToken.toString());
        store.store(new FileOutputStream(secure_file), store_pass);
    }

    /**
     * Retrieves all {@link ScopedAuthenticationToken}s in the secure store. A {@link ScopedAuthenticationToken}
     * must be composed of both a user name and a password, i.e. items in the secure store that are not
     * considered to be a part of an authentication token are ignored.
     * @return List of {@link ScopedAuthenticationToken}s.
     * @throws Exception
     */
    public List<ScopedAuthenticationToken> getAuthenticationTokens() throws Exception{
        List<String> allAliases = Collections.list(store.aliases());
        List<ScopedAuthenticationToken> allScopedAuthenticationTokens = matchEntries(allAliases);

        return allScopedAuthenticationTokens;
    }

    /**
     * Locates all items in the secure store suffixed by {@link #USERNAME_TAG}, and optionally prefixed by a string
     * identifying a "scope". For each such item a matching password item - suffixed by {@link #PASSWORD_TAG} is
     * retrieved. {@link #USERNAME_TAG} is considered representing an authentication only if a matching
     * {@link #PASSWORD_TAG} item can be found.
     * @param aliases All aliases in the secure store.
     * @return List of {@link ScopedAuthenticationToken}s.
     * @throws Exception
     */
    private List<ScopedAuthenticationToken> matchEntries(List<String> aliases) throws Exception{
        List<ScopedAuthenticationToken> allScopedAuthenticationTokens
                = new ArrayList<>();
        for(String alias : aliases){
            if(alias.endsWith(PASSWORD_TAG)){
                continue;
            }
            String[] tokens = alias.split("\\.");
            String username;
            String password;
            String scope = null;
            // Non-scoped alias?
            if(tokens.length == 1 && USERNAME_TAG.equals(tokens[0])){
                // It is assumed that the secure store can contain zero or one entries named "username" or "password".
                // It is further assumed that these are "matching" items, i.e. they constitute a non-scoped authentication
                // token that was persisted at some point.
                username = get(tokens[0]);
                password = get(PASSWORD_TAG);
            }
            else{
                scope = tokens[0];
                username = get(scope + "." + USERNAME_TAG);
                password = get(scope + "." + PASSWORD_TAG);
            }
            // Add only if password was found.
            if(password != null){
                allScopedAuthenticationTokens.add(new ScopedAuthenticationToken(scope, username, password));
            }
        }
        return allScopedAuthenticationTokens;
    }

}
