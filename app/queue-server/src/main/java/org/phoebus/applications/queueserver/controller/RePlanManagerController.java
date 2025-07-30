package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.view.TabSwitchEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.net.URL;
import java.util.ResourceBundle;

public class RePlanManagerController implements Initializable {

    @FXML private TabPane tabPane;
    @FXML private Tab planViewerTab;
    @FXML private Tab planEditorTab;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TabSwitchEvent.getInstance().addListener(this::switchToTab);
    }

    private void switchToTab(String tabName) {
        if (tabPane == null) return;

        switch (tabName) {
            case "Plan Editor" -> tabPane.getSelectionModel().select(planEditorTab);
            case "Plan Viewer" -> tabPane.getSelectionModel().select(planViewerTab);
        }
    }

    public String getCurrentTabName() {
        if (tabPane == null || tabPane.getSelectionModel().getSelectedItem() == null) {
            return null;
        }
        return tabPane.getSelectionModel().getSelectedItem().getText();
    }
}