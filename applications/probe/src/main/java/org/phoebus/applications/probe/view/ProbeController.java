package org.phoebus.applications.probe.view;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionChangeListener;
import org.phoebus.framework.selection.SelectionService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class ProbeController {

    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

    @FXML
    Button btncalculate;
    @FXML
    TextField txtPVName;
    @FXML
    TextField txtValue;
    @FXML
    TextField txtTimeStamp;

    @FXML
    public void initialize() {
        // register selection listener
        SelectionService.addListener(new SelectionChangeListener() {

            @Override
            public void selectionChanged(Object source, Selection oldValue, Selection newValue) {
                if (source.equals(txtPVName)) {
                    System.out.println("I set the selection to : " + newValue);
                } else {
                    System.out.println("The new selection is : " + newValue);
                }
            }
        });
    }

    @FXML
    private void setSelection() {
        SelectionService.setSelection(txtPVName, Arrays.asList(txtPVName.getText()));
    }

    @FXML
    private void search() {
        // search for pv
        txtValue.setText(txtPVName.getText() + ": Value");
        txtTimeStamp.setText(timeFormat.format(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())));
    }
}
