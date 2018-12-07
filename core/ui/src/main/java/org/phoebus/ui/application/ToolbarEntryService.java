package org.phoebus.ui.application;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.phoebus.ui.spi.ToolbarEntry;

public class ToolbarEntryService {

    private static ToolbarEntryService toolbarEntryService;
    private ServiceLoader<ToolbarEntry> loader;
    private List<ToolbarEntry> toolbarEntries;

    private ToolbarEntryService() {
        loader = ServiceLoader.load(ToolbarEntry.class);
        toolbarEntries = loader.stream().map(Provider::get).collect(Collectors.toList());
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
        return toolbarEntries;
    } 
    
}
