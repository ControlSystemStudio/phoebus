package org.csstudio.trends.databrowser3.ui.properties;

import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.ui.DroppedPVNameParser;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.csstudio.trends.databrowser3.ui.plot.PlotListener;
import org.phoebus.ui.dialog.DialogHelper;

public class AddPVsFromTheClipboardMenuItem extends MenuItem {
    ModelBasedPlot modelBasedPlot;
    public AddPVsFromTheClipboardMenuItem(ModelBasedPlot modelBasedPlot) {
        super("Add PV(s) from the Clipboard",
                Activator.getIcon("paste"));

        this.modelBasedPlot = modelBasedPlot;

        setOnAction(actionEvent -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                String clipboardContents = clipboard.getString();
                try {
                    var pvNameAndDisplayNames = DroppedPVNameParser.parseDroppedPVs(clipboardContents);
                    if (pvNameAndDisplayNames.size() > 0) {
                        PlotListener plotListener = this.modelBasedPlot.getListener(); // Cannot call modelBasedPlot.getListener() in the constructor, because getListener() may return "null" at that stage.
                        if (plotListener != null) {
                            plotListener.droppedNames(pvNameAndDisplayNames);
                        }
                    }
                    else {
                        showNoPVsFoundInClipboardWarning();
                    }
                }
                catch (Exception exception) {
                    showNoPVsFoundInClipboardWarning();
                }
            }
            else {
                showNoPVsFoundInClipboardWarning();
            }
        });
    }

    private void showNoPVsFoundInClipboardWarning() {
        Alert warningAlert = new Alert(Alert.AlertType.INFORMATION,
                                       "The clipboard doesn't contain a list of PV names separated by newline-characters:\n\n<PV Name1>\n<PV Name2>\n    ⋮\n\nOptionally, display names can be specified after a comma following the PV name:\n\n<PV Name 1>,<Display Name 1>\n<PV Name 2>,<Display Name 2>\n    ⋮");
        warningAlert.setHeaderText("No PVs found in the clipboard!");
        DialogHelper.positionDialog(warningAlert, modelBasedPlot.getPlot(), 0, 0);
        warningAlert.show();
    }
}
