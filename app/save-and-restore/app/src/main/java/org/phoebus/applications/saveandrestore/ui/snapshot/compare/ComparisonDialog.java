/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.framework.nls.NLS;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Dialog showing a {@link javafx.scene.control.TableView} where array or table data is visualized element wise.
 * Purpose is to be able to inspect deltas on array/table element level.
 *
 * <p>
 *     Data in the {@link javafx.scene.control.TableView} is organized column wise. Each row in the {@link javafx.scene.control.TableView}
 *     corresponds to an individual element in the data. Each column contains three nested columns: stored value,
 *     delta and live value.
 * </p>
 * <p>
 *     For an array type ({@link org.epics.vtype.VNumberArray} the table will thus hold a single data column. For a
 *     table type ({@link org.epics.vtype.VTable} there will be one data column for each column in the table.
 * </p>
 */
public class ComparisonDialog extends Dialog {

    /**
     * Constructor
     * @param data The data as stored in a {@link org.phoebus.applications.saveandrestore.model.Snapshot}
     * @param pvName The name of the for which
     */
    public ComparisonDialog(VType data, String pvName){

        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        setResizable(true);
        setTitle(Messages.comparisonDialogTitle);

        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(this.getClass().getResource("TableComparisonView.fxml"));
        try {
            Node node = loader.load();
            TableComparisonViewController controller = loader.getController();
            controller.loadDataAndConnect(data, pvName);
            getDialogPane().setContent(node);
            setOnCloseRequest(e -> controller.cleanUp());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
