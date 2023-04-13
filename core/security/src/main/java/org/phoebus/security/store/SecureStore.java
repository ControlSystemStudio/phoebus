/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.security.store;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.security.PhoebusSecurity;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

/**
 * Handles reading/writing username, passsword, and token data. Internally delegates to
 * a Store implementation that handles storage of that data.
 */
@SuppressWarnings("nls")
public class SecureStore
{

    private final Store<String, String> store;

    /** Tags */
    public static final String USERNAME_TAG = "username",
                               PASSWORD_TAG = "password";

    private static final Logger LOGGER = Logger.getLogger(SecureStore.class.getName());

    /**
     * Default constructor, self-initializes underlying store based on
     * security preferences.
     *
     * @see {@link org.phoebus.security.PhoebusSecurity}
     *
     * @throws Exception if underlying store isn't configured, configured incorrectly.
     * See javadocs for underlying implementations for details.
     */
    public SecureStore() throws Exception
    {
        switch(PhoebusSecurity.secure_store_target) {
            case FILE:
            default:
                store = new FileBasedStore();
                break;
            case IN_MEMORY:
                store = MemoryBasedStore.getInstance();
                break;
        }
    }

    /**
     * Initialize with specified store implementation.
     * @param store Underlying store implementation.
     */
    SecureStore(Store<String, String> store) {
        this.store = store;
    }

    /** Read an entry from the store
     *  @param tag Tag that identifies the entry
     *  @return Stored text or <code>null</code>
     *  @throws Exception on error
     */
    public String get(final String tag) throws Exception
    {
        return store.get(tag);
    }

    /** Write an entry to the store. If the entry already exists, it will be overwritten.
     *  @param tag Tag that identifies the entry
     *  @param value Value of the entry
     *  @throws Exception on error
     */
    public void set(final String tag, final String value) throws Exception
    {
        store.set(tag, value);
    }

    /** Deletes an entry in the secure store.
     *  @param tag The tag to delete.
     *  @throws Exception on error
     */
    public void delete(String tag) throws Exception{
        LOGGER.log(Level.INFO, "Deleting entry " + tag + " from secure store");
        store.delete(tag);
    }

    /** @param scope Scope identifier, will be converted to lower case, see {@link ScopedAuthenticationToken}
     *  @return Token for that scope
     *  @throws Exception on error
     */
    public ScopedAuthenticationToken getScopedAuthenticationToken(String scope) throws Exception{
        String username;
        String password;
        if(scope == null || scope.trim().isEmpty()){
            username = get(USERNAME_TAG);
            password = get(PASSWORD_TAG);
        }
        else{
            scope = scope.toLowerCase();
            username = get(scope + "." + USERNAME_TAG);
            password = get(scope + "." + PASSWORD_TAG);
        }
        if(username == null || password == null){
            return null;
        }
        return new ScopedAuthenticationToken(scope, username, password);
    }

    /** @param scope Scope identifier, will be converted to lower case, see {@link ScopedAuthenticationToken}
     *  @throws Exception on error
     */
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

    /** @throws Exception on error */
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

    /** @param scopedAuthenticationToken Token for scope
     *  @throws Exception on error
     */
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
        LOGGER.log(Level.INFO, "Storing scoped authentication token " + scopedAuthenticationToken);
    }

    /**
     * Retrieves all {@link ScopedAuthenticationToken}s in the secure store. A {@link ScopedAuthenticationToken}
     * must be composed of both a user name and a password, i.e. items in the secure store that are not
     * considered to be a part of an authentication token are ignored.
     * @return List of {@link ScopedAuthenticationToken}s.
     * @throws Exception on error
     */
    public List<ScopedAuthenticationToken> getAuthenticationTokens() throws Exception{
        List<String> allAliases = new ArrayList<>(store.getKeys());
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
     * @throws Exception If interaction with the underlying store implementation fails.
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
