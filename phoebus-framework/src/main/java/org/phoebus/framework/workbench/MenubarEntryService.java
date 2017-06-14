package org.phoebus.framework.workbench;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.phoebus.framework.spi.MenuEntry;

public class MenubarEntryService {

    private static MenubarEntryService menubarEntryService;
    private ServiceLoader<MenuEntry> loader;

    private MenubarEntryService() {
        loader = ServiceLoader.load(MenuEntry.class);
    }

    public static synchronized MenubarEntryService getInstance() {
        if (menubarEntryService == null) {
            menubarEntryService = new MenubarEntryService();
        }
        return menubarEntryService;
    }

    /**
     * Get the list of registered toolbar entries
     * @return
     */
    public List<MenuEntry> listToolbarEntries(){
        return loader.stream().map(Provider::get).collect(Collectors.toList());
    } 
    
}
