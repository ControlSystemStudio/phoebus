package org.phoebus.security.store;

import org.phoebus.security.tokens.ScopedAuthenticationToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes entries to an in-memory map that persists during the lifetime the application is running.
 */
public class MemoryBasedStore implements Store<String, String> {

    private final Map<String, String> store = new HashMap<>();

    private static MemoryBasedStore INSTANCE = new MemoryBasedStore();

    private MemoryBasedStore() {}

    public static MemoryBasedStore getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        return store.get(key);
    }

    @Override
    public List<String> getKeys() {
        return new ArrayList<>(store.keySet());
    }

    @Override
    public void set(String key, String value) {
        store.put(key, value);
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }
}
