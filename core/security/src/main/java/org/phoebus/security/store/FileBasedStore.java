package org.phoebus.security.store;

import org.phoebus.framework.workbench.Locations;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Secure Store
 *
 * <p>Writes tag/value pairs into an encrypted file.
 *
 * <p>Does depend on a password to read and write the file.
 *
 * @author Kay Kasemir
 */
public class FileBasedStore implements Store<String, String> {

    private final SecretKeyFactory kf = SecretKeyFactory.getInstance("PBE");
    private final KeyStore store;
    private final File secure_file;
    private byte[] secureStoreByteArray;
    private final char[] store_pass;
    private final KeyStore.ProtectionParameter pp;

    private static final Logger LOGGER = Logger.getLogger(SecureStore.class.getName());

    /**
     * Create with default file in 'user' location
     *
     * @throws Exception on error
     */
    public FileBasedStore() throws Exception {
        this(new File(Locations.user(), "secure_store.dat"));
    }

    /**
     * Create with default password.
     *
     * <p>Knowledge of this code would allow reading the file
     *
     * @param secure_file File to read or write
     * @throws Exception on error
     */
    public FileBasedStore(final File secure_file) throws Exception {
        this(secure_file, Integer.toString(secure_file.getAbsolutePath().hashCode()).toCharArray());
    }

    /**
     * Create
     *
     * @param secure_file File to read or write
     * @param store_pass  Password for encoding/decoding entries
     * @throws Exception on error
     */
    public FileBasedStore(final File secure_file, final char[] store_pass) throws Exception {
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

    /**
     * Read an entry from the store
     *
     * @param tag Tag that identifies the entry.
     * @return Stored text or <code>null</code>, e.g. if tag is <code>null</code> or tag is not associated with
     * a key in the secure store file.
     * @throws Exception on error.
     */
    @Override
    public String get(final String tag) throws Exception {
        if (tag == null) {
            return null;
        }
        final KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) store.getEntry(tag, pp);
        if (entry == null)
            return null;

        final PBEKeySpec key = (PBEKeySpec) kf.getKeySpec(entry.getSecretKey(), PBEKeySpec.class);
        return new String(key.getPassword());
    }

    /**
     * Write an entry to the store. If the entry already exists, it will be overwritten.
     *
     * @param tag   Tag that identifies the entry
     * @param value Value of the entry
     * @throws Exception on error
     */
    @Override
    public void set(final String tag, final String value) throws Exception {
        final SecretKey skey = kf.generateSecret(new PBEKeySpec(value.toCharArray()));
        store.setEntry(tag, new KeyStore.SecretKeyEntry(skey), pp);

        // Write file whenever an entry is changed
        store.store(new FileOutputStream(secure_file), store_pass);
    }

    /**
     * Deletes an entry in the secure store.
     *
     * @param tag The tag to delete. If <code>null</code> this method does not do anything.
     * @throws Exception on error
     */
    @Override
    public void delete(String tag) throws Exception {
        if (tag == null) {
            return;
        }
        store.deleteEntry(tag);
        LOGGER.log(Level.INFO, "Deleting entry " + tag + " from secure store");
        // Write file whenever an entry is changed
        store.store(new FileOutputStream(secure_file), store_pass);
    }

    @Override
    public List<String> getKeys() throws Exception {
        return Collections.list(store.aliases());
    }

}
