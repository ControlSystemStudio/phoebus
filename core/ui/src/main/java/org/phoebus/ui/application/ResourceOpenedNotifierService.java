package org.phoebus.ui.application;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ResourceOpenedNotifierService {

    static final List<ResourceOpenedListener> listeners = new ArrayList<>();

    public static void addListener(ResourceOpenedListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(ResourceOpenedListener listener) {
        listeners.remove(listener);
    }

    public static void notifyListeners(String resourceName, String fromWho) {
        for (ResourceOpenedListener listener : listeners) {
            listener.notifyResourceOpened(resourceName, fromWho);
        }
    }

}