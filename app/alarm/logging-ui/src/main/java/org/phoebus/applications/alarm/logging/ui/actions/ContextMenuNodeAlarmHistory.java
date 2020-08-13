package org.phoebus.applications.alarm.logging.ui.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.applications.alarm.logging.ui.AlarmLogTable;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableApp;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;

/**
 * A headless context menu entry for creating log entries from adaptable
 * selections. TODO this temporary headless action needs to removed once the
 * create log entry dialog is complete.
 *
 * @author Tanvi Ashwarya
 *
 */
@SuppressWarnings("rawtypes")
public class ContextMenuNodeAlarmHistory implements ContextMenuEntry {

    private static final Class<?> supportedType = AlarmTreeItem.class;
    private static final String NAME = "Alarm History";

    @Override
    public String getName() {
        return NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void call(Selection selection) throws URISyntaxException {

        List<AlarmTreeItem> selectedNodes = new ArrayList<>();
        selection.getSelections().stream().forEach(s -> {
            AdapterService.adapt(s, AlarmTreeItem.class).ifPresent(selectedNodes::add);
        });
        AlarmLogTable table = ApplicationService.createInstance(AlarmLogTableApp.NAME);
        URI uri = new URI(AlarmLogTableApp.SUPPORTED_SCHEMA, "", "", "node="+selectedNodes.stream().map(AlarmTreeItem::getName).collect(Collectors.joining(",")), "");
        table.setNodeResource(uri);
    }

    @Override
    public Class<?> getSupportedType() {
        return supportedType;
    }

    @Override
    public Image getIcon() {
        return AlarmLogTableApp.icon;
    }
}
