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

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.phoebus.framework.workbench.Locations;

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

    /** Write an entry to the store
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

//    public static void main(String[] args) throws Exception
//    {
//        // Standalone test needs to initialize locations
//        Locations.initialize();
//        final SecureStore store = new SecureStore();
//        final String value = store.get("test");
//        System.out.println(value);
//        if (value == null)
//            store.set("test", "demo");
//        else
//            store.set("test", value + "_x");
//    }
}
