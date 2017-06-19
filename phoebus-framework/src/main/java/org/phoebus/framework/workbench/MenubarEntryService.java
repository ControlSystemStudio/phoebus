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
public class MenubarEntryService {

    private static MenubarEntryService menubarEntryService;
    private ServiceLoader<MenuEntry> loader;

    private List<MenuEntry> menuEntries = Collections.emptyList();

    private MenubarEntryService() {
        loader = ServiceLoader.load(MenuEntry.class);
        loader.stream().map(Provider::get).collect(Collectors.toList());
    }

    public static synchronized MenubarEntryService getInstance() {
        if (menubarEntryService == null) {
            menubarEntryService = new MenubarEntryService();
        }
        return menubarEntryService;
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
