package org.csstudio.display.builder.runtime.internal;

import javafx.scene.Node;
import org.csstudio.display.actions.WritePVAction;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.scan.client.ScanInfoModel;
import org.csstudio.scan.ui.editor.ScheduledScanDialog;
import org.phoebus.ui.dialog.DialogHelper;

import java.util.*;


public class ActionButtonWidgetRuntime extends WidgetRuntime<ActionButtonWidget> {

    private class ScheduledTaskDialogAction extends RuntimeAction {
        private final String script_xml;

        public ScheduledTaskDialogAction(String scriptXml) {
            super("Schedule Task", "/icons/clock.png");
            script_xml = scriptXml;
        }

        @Override
        public void run() {
            try {
                ScheduledScanDialog dialog = new ScheduledScanDialog(
                        "Scheduled Task",
                        ScanInfoModel.getInstance().getScanClient(),
                        script_xml
                );
                final Node node = JFXBaseRepresentation.getJFXNode(widget);
                DialogHelper.positionDialog(dialog, node, 0, 0);
                dialog.showAndWait();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Collection<RuntimeAction> getRuntimeActions() {
        List<String> commands = widget.propActions().getValue().getActions()
                .stream()
                .filter(action -> action instanceof WritePVAction)
                .map(action -> {
                    WritePVAction writePVAction = (WritePVAction) action;
                    String pvName = writePVAction.formatPv(widget);
                    String value = writePVAction.formatValue(widget);
                    return (
                            "<set>" +
                                "<device>" + pvName + "</device>" +
                                "<value>" + value + "</value>" +
                            "</set>"
                    );
                }).toList();

        if (commands.isEmpty()) {
            return List.of();
        }

        return List.of(new ScheduledTaskDialogAction("<commands>" + String.join("", commands) + "</commands>"));
    }
}
