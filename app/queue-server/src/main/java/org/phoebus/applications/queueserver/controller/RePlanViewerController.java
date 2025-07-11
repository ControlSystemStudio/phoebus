package org.phoebus.applications.queueserver.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

public class RePlanViewerController {

    @FXML
    private TableColumn<?, ?> chkCol;

    @FXML
    private HBox editBtn;

    @FXML
    private CheckBox paramChk;

    @FXML
    private TableColumn<?, ?> paramCol;

    @FXML
    private Label planLabel;

    @FXML
    private TableView<?> table;

    @FXML
    private TableColumn<?, ?> valueCol;

}
