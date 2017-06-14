package org.phoebus.applications.greetings.ui;

import static javafx.collections.FXCollections.observableArrayList;

import java.util.stream.Collectors;

import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionChangeListener;
import org.phoebus.framework.selection.SelectionService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class GreetingController {

    @FXML
    Button btncalculate;
    @FXML
    ListView<String> listView;

    @FXML
    public void initialize() {
        // register selection listener
        SelectionService.addListener(new SelectionChangeListener() {

            @Override
            public void selectionChanged(Object source, Selection oldValue, Selection newValue) {
                if (!source.equals(listView)) {
                    listView.setItems(observableArrayList(
                            newValue.getSelections().stream().map(Object::toString).collect(Collectors.toList())));
                }
            }
        });
    }
}
