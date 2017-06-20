package org.phoebus.framework.workbench;

import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.spi.ContextMenuEntry;

@SuppressWarnings("rawtypes")
public class ContextMenuService {

    private static ContextMenuService contextMenuService;
    private static ServiceLoader<ContextMenuEntry> loader;
    private List<ContextMenuEntry> contextMenuEntries;

    private ContextMenuService() {
        loader = ServiceLoader.load(ContextMenuEntry.class);
        contextMenuEntries = Collections
                .unmodifiableList(loader.stream().map(Provider::get).collect(Collectors.toList()));
    }

    public static synchronized ContextMenuService getInstance() {
        if (contextMenuService == null) {
            contextMenuService = new ContextMenuService();
        }
        return contextMenuService;
    }

    /**
     * Get the list of registered context menu providers
     * 
     * @return
     */
    public List<ContextMenuEntry> listContextMenuEntries() {
        return contextMenuEntries;
    }

    /**
     * 
     * @param selectionType
     * @return
     */
    public List<ContextMenuEntry> listSupportedContextMenuEntries() {
        List<Class> selectionTypes = SelectionService.getInstance().getSelection().getSelections().stream().map(s -> {
            return s.getClass();
        }).collect(Collectors.toList());

        return contextMenuEntries.stream().filter(p -> {
            return !Collections.disjoint(p.getSupportedTypes(), selectionTypes);
        }).collect(Collectors.toList());

    }
}
