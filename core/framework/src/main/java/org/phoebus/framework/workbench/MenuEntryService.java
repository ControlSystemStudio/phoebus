package org.phoebus.framework.workbench;

import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.phoebus.framework.spi.MenuEntry;

/**
 * 
 * @author Kunal Shroff
 *
 */
public class MenuEntryService {

    private static MenuEntryService menuEntryService;
    private ServiceLoader<MenuEntry> loader;

    private List<MenuEntry> menuEntries = Collections.emptyList();

    private MenuEntryService() {
        loader = ServiceLoader.load(MenuEntry.class);
        menuEntries = loader.stream().map(Provider::get).collect(Collectors.toList());
    }

    public static synchronized MenuEntryService getInstance() {
        if (menuEntryService == null) {
            menuEntryService = new MenuEntryService();
        }
        return menuEntryService;
    }

    /**
     * Get the list of registered menu entries
     * 
     * @return
     */
    public List<MenuEntry> listToolbarEntries() {
        return menuEntries;
    }

}
