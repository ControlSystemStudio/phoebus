package org.phoebus.logging.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.spi.ContextMenuEntry;
import org.phoebus.logging.LogEntry;
import org.phoebus.logging.LogService;

@SuppressWarnings("rawtypes")
@ProviderFor(ContextMenuEntry.class)
public class contextMenuLogging implements ContextMenuEntry {

    private static final String NAME = "Create Log";
    private static final List<Class> supportedTypes = Arrays.asList(LogEntry.class);

    public String getName() {
        return NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object callWithSelection(Selection selection) {

        List<LogEntry> adaptedSelections = new ArrayList<LogEntry>();
        selection.getSelections().stream().forEach(s -> {
            AdapterService.getInstance().getAdaptersforAdaptable(s.getClass()).ifPresent(a -> {
                a.forEach(af -> {
                    af.getAdapter(s, LogEntry.class).ifPresent(adapted -> {
                        adaptedSelections.add((LogEntry) adapted);
                    });
                });
            });
        });
        LogService.getInstance().createLogEntry(adaptedSelections);
        return null;
    }

    @Override
    public Object getIcon() {
        return null;
    }

    @Override
    public List<Class> getSupportedTypes() {
        return supportedTypes;
    }

}
