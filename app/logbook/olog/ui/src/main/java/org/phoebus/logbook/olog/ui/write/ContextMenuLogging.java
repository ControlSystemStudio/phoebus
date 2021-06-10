package org.phoebus.logbook.olog.ui.write;

import javafx.scene.Node;
import javafx.scene.image.Image;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.olog.ui.menu.SendToLogBookApp;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * A headless context menu entry for creating log entries from adaptable selections.
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

    @Override
    public Image getIcon() {
        return SendToLogBookApp.icon;
    }

    /**
     *
     * @param parent the parent Node
     * @param selection The selection to be used to execute this action
     */
    public void call(Node parent, Selection selection) {
        List<LogEntry> adaptedSelections = new ArrayList<>();
        selection.getSelections().stream().forEach(s -> {
            AdapterService.adapt(s, LogEntry.class).ifPresent(adapted -> {
                adaptedSelections.add(adapted);
            });
        });
        new LogEntryEditorStage(parent, adaptedSelections.get(0), null).show();
    }

    @Override
    public Class<?> getSupportedType() {
        return supportedType;
    }

}
