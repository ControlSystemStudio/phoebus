package org.csstudio.trends.databrowser3.ui.properties;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.DroppedPVNameParser;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;

public class AddPVsFromTheClipboardMenuItem extends MenuItem {
    Node nodeToPositionDialogOver;
    public AddPVsFromTheClipboardMenuItem(UndoableActionManager undoableActionManager,
                                          Model model,
                                          Node nodeToPositionDialogOver) {
        super(Messages.AddPVsFromTheClipboard,
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
                                       Messages.TheClipboardDoesNotContainPVs);
        warningAlert.setTitle(Messages.NoPVsFoundInTheClipboard);
        warningAlert.setHeaderText(Messages.NoPVsFoundInTheClipboard);
        DialogHelper.positionDialog(warningAlert, nodeToPositionDialogOver, 0, 0);
        warningAlert.show();
    }
}
