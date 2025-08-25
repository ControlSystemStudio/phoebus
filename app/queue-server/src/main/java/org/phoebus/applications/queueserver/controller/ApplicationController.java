package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.view.ViewFactory;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public final class ApplicationController implements Initializable {

    @FXML private Tab monitorQueueTab;
    @FXML private Tab editAndControlQueueTab;
    
    private static final Logger LOG = Logger.getLogger(ApplicationController.class.getName());

    @Override public void initialize(URL url, ResourceBundle rb) {
        LOG.info("Initializing ApplicationController");
        monitorQueueTab.setContent(ViewFactory.MONITOR_QUEUE.get());
        editAndControlQueueTab.setContent(ViewFactory.EDIT_AND_CONTROL_QUEUE.get());
        LOG.info("ApplicationController initialization complete");
    }

}
