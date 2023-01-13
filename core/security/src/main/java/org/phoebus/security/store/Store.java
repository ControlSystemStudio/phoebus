package org.phoebus.security.store;

import java.util.List;

public interface Store<K, V> {

    V get(K key) throws Exception;
    void set(K key, V value) throws Exception;
    List<K> getKeys() throws Exception;
    void delete(K key) throws Exception;

}
