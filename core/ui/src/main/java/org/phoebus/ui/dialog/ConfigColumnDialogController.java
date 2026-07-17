package org.phoebus.ui.dialog;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import org.phoebus.ui.javafx.ColumnConfigHandler;

public class ConfigColumnDialogController {

    private ColumnConfigHandler colConfHandler;

    @FXML
    private VBox columns;

    public ConfigColumnDialogController(ColumnConfigHandler colConfHandler){
        this.colConfHandler = colConfHandler;
    }

    public void initialize(){

        columns.setPadding(new Insets(10));
        columns.setSpacing(8);

        for(TableColumn<?, ?> col : colConfHandler.getConfigurableColumns()){
            CheckBox columnBox = new CheckBox(col.getText());
            columns.getChildren().addAll(columnBox);
            columnBox.setSelected(col.isVisible());
            columnBox.selectedProperty().addListener(
                    (obs, o, n) -> {
                        colConfHandler.setVisibility(col, n);
                    }
            );
        }
    }
}
