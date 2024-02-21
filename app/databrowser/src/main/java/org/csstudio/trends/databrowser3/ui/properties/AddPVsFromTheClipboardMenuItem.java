package org.csstudio.trends.databrowser3.ui.properties;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.DroppedPVNameParser;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;

public class AddPVsFromTheClipboardMenuItem extends MenuItem {
    Node nodeToPositionDialogOver;
    public AddPVsFromTheClipboardMenuItem(UndoableActionManager undoableActionManager,
                                          Model model,
                                          Node nodeToPositionDialogOver) {
        super("Add PV(s) from the Clipboard",
                Activator.getIcon("paste"));

        this.nodeToPositionDialogOver = nodeToPositionDialogOver;

        setOnAction(actionEvent -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                String clipboardContents = clipboard.getString();
                try {
                    var pvNameAndDisplayNames = DroppedPVNameParser.parseDroppedPVs(clipboardContents);
                    if (pvNameAndDisplayNames.size() > 0) {
                        Activator.addPVsToPlotDialog(pvNameAndDisplayNames,
                                                     undoableActionManager,
                                                     model,
                                                     nodeToPositionDialogOver);
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
        DialogHelper.positionDialog(warningAlert, nodeToPositionDialogOver, 0, 0);
        warningAlert.show();
    }
}
