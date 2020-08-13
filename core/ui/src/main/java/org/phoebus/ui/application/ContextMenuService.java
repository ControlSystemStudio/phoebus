package org.phoebus.ui.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.spi.ContextMenuEntry;

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
     * Using the current selection from the {@link SelectionService} and checking
     * the {@link AdapterService} for all additionally supported types via adapters.
     * The {@link ContextMenuService} returns a list of context menu actions
     * supported for the current selection.
     * 
     * @return A list of {@link ContextMenuEntry}'s supported for the current
     *         selection
     */
    public List<ContextMenuEntry> listSupportedContextMenuEntries() {
        // List of types of the current selection
        List<Class> selectionTypes = SelectionService.getInstance().getSelection().getSelections().stream().map(s -> {
            return s.getClass();
        }).collect(Collectors.toList());

        // Take into account the types the selected objects can be converted into
        List<Class> allAdaptableSelectionType = new ArrayList<>();
        selectionTypes.forEach(s -> {
            // Class can certainly be converted to the class itself,
            // but also to all its super classes
            Class sc = s;
            do
            {
                allAdaptableSelectionType.add(sc);
                for (Class inter : sc.getInterfaces())
                {
                    allAdaptableSelectionType.add(inter);
                }
                sc = sc.getSuperclass();
            }
            while (sc != Object.class);

            AdapterService.getAdaptersforAdaptable(s).stream().forEach(f -> {
                allAdaptableSelectionType.addAll(f.getAdapterList());
            });
        });

        // Return the context menu entries that can handle the given selection type,
        // either directly or by adapting it to a supported type.
        final List<ContextMenuEntry> result = contextMenuEntries
            .stream()
            .filter(p -> {
                return allAdaptableSelectionType.contains(p.getSupportedType());
            })
            .collect(Collectors.toList());
        result.sort((a, b) -> a.getName().compareTo(b.getName()));
        return result;
    }
}
