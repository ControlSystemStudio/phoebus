package org.phoebus.applications.alarm.logging.ui.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.applications.alarm.logging.ui.AlarmLogTable;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableApp;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;

/**
 * A headless context menu entry for creating log entries from adaptable
 * selections.
 *
 * @author Kunal Shroff
 *
 */
public class ContextMenuPVAlarmHistory implements ContextMenuEntry {

    private static final Class<?> supportedType = ProcessVariable.class;
    private static final String NAME = "Alarm History";

    @Override
    public String getName() {
        return NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void call(Selection selection) throws URISyntaxException {

        List<ProcessVariable> selectedPvs = new ArrayList<>();
        selection.getSelections().stream().forEach(s -> {
            AdapterService.adapt(s, ProcessVariable.class).ifPresent(selectedPvs::add);
        });
        AlarmLogTable table = ApplicationService.createInstance(AlarmLogTableApp.NAME);
        URI uri = new URI(AlarmLogTableApp.SUPPORTED_SCHEMA, "", "", "pv="+selectedPvs.stream().map(ProcessVariable::getName).collect(Collectors.joining(",")), "");
        table.setPVResource(uri);
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
