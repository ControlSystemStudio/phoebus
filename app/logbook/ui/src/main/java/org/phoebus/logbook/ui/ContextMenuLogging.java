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
public class ContextMenuLogging implements ContextMenuEntry {

    private static final String NAME = "Create Log";
    private static final Class<?> supportedType = LogEntry.class;

    @Override
    public String getName() {
        return NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void call(Selection selection) {

        List<LogEntry> adaptedSelections = new ArrayList<>();
        selection.getSelections().stream().forEach(s -> {
            AdapterService.adapt(s, LogEntry.class).ifPresent(adapted -> {
                adaptedSelections.add(adapted);
            });
        });
        LogService.getInstance().createLogEntry(adaptedSelections, null);
    }

    @Override
    public Class<?> getSupportedType() {
        return supportedType;
    }

}
