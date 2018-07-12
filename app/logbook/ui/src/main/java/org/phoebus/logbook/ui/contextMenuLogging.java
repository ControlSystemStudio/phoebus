package org.phoebus.logbook.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogService;
import org.phoebus.ui.spi.ContextMenuEntry;

/**
 * A headless context menu entry for creating log entries from adaptable selections.
 * TODO this temporary headless action needs to removed once the create log entry dialog is complete. 
 * @author Kunal Shroff
 *
 */
@SuppressWarnings("rawtypes")
public class contextMenuLogging implements ContextMenuEntry {

    private static final String NAME = "Create Log";
    private static final List<Class> supportedTypes = Arrays.asList(LogEntry.class);

    @Override
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
    public List<Class> getSupportedTypes() {
        return supportedTypes;
    }

}
