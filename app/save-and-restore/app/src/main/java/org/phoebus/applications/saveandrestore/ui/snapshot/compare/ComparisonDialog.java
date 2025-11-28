/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.framework.nls.NLS;

import java.io.IOException;
import java.util.ResourceBundle;

public class ComparisonDialog extends Dialog {

    public ComparisonDialog(VType data, String pvName){

        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(this.getClass().getResource("TableComparisonView.fxml"));
        try {
            Node node = loader.load();
            TableComparisonViewController controller = loader.getController();
            controller.loadDataAndConnect(data, pvName);
            getDialogPane().setContent(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
