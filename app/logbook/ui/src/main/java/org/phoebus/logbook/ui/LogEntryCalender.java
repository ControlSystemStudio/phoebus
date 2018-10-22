package org.phoebus.logbook.ui;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.utility.LogbookSearchJob;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.Appointment;
import jfxtras.scene.control.agenda.Agenda.AppointmentImplLocal;

public class LogEntryCalender implements AppInstance {

    final static Logger log = Logger.getLogger(LogEntryCalender.class.getName());

    private final LogEntryCalenderApp app;
    private DockItem tab;

    private LogEntryCalenderViewController controller;


    LogEntryCalender(final LogEntryCalenderApp app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("LogEntryCalenderView.fxml"));
            loader.load();
            controller = loader.getController();
            if (this.app.getClient() != null) {
                controller.setClient(this.app.getClient());
            } else {
                log.log(Level.SEVERE, "Failed to acquire a valid logbook client");
            }

            tab = new DockItem(this, loader.getRoot());
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot load UI", e);
        }
        tab.setOnClosed(event -> {
            // dispose();
        });
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return app;
    }

}
