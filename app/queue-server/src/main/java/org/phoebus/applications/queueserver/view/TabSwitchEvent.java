package org.phoebus.applications.queueserver.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TabSwitchEvent {
    private static final TabSwitchEvent instance = new TabSwitchEvent();
    private final List<Consumer<String>> listeners = new ArrayList<>();
    private static final Logger LOG = Logger.getLogger(TabSwitchEvent.class.getName());

    private TabSwitchEvent() {}

    public static TabSwitchEvent getInstance() {
        return instance;
    }

    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void switchToTab(String tabName) {
        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(tabName);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Tab switch listener error", e);
            }
        }
    }
}