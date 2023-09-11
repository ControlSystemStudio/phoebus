package org.phoebus.security.store;

import java.util.List;

/**
 * Interface Store
 * @param <K> Key Type
 * @param <V> Value Type
 */
public interface Store<K, V> {

    /**
     * @param key key
     * @return Value value
     * @throws Exception on error
     */
	V get(K key) throws Exception;
	/**
	 * @param key key
	 * @param value value
	 * @throws Exception on error
	 */
    void set(K key, V value) throws Exception;
    /**
     * @return list of keys
     * @throws Exception on error
     */
    List<K> getKeys() throws Exception;
    /**
     * @param key key
     * @throws Exception on error
     */
    void delete(K key) throws Exception;

}
