package org.phoebus.framework.workbench;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.phoebus.framework.spi.ToolbarEntry;

public class ToolbarEntryService {

    private static ToolbarEntryService toolbarEntryService;
    private ServiceLoader<ToolbarEntry> loader;

    private ToolbarEntryService() {
        loader = ServiceLoader.load(ToolbarEntry.class);
    }

    public static synchronized ToolbarEntryService getInstance() {
        if (toolbarEntryService == null) {
            toolbarEntryService = new ToolbarEntryService();
        }
        return toolbarEntryService;
    }

    /**
     * Get the list of registered toolbar entries
     * @return
     */
    public List<ToolbarEntry> listToolbarEntries(){
        return loader.stream().map(Provider::get).collect(Collectors.toList());
    } 
    
}
