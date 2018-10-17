package org.phoebus.logbook.ui;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
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
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.Appointment;
import jfxtras.scene.control.agenda.Agenda.AppointmentImplLocal;

public class LogEntryCalender extends LogbookSearchController implements AppInstance {
    private final LogEntryCalenderApp app;
    private DockItem tab;

    private Agenda agenda;
    private LogEntryControl logEntryControl;

    // Model
    private Map<Appointment, LogEntry> map;
    private Map<String, Agenda.AppointmentGroup> appointmentGroupMap = new TreeMap<String, Agenda.AppointmentGroup>();
    private List<String> groups;
    private List<LogEntry> logEntries;

    LogEntryCalender(final LogEntryCalenderApp app) {
        this.app = app;
        tab = new DockItem(this, createFxScene());
        DockPane.getActiveDockPane().addTab(tab);

        tab.setOnClosed(event -> {
            // dispose();
        });
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return app;
    }

    protected AnchorPane createFxScene() {
        AnchorPane anchorpane = new AnchorPane();
        agenda = new Agenda();
        agenda.setEditAppointmentCallback(new Callback<Agenda.Appointment, Void>() {

            @Override
            public Void call(Appointment appointment) {
                return null;
            }
        });

        agenda.setActionCallback((appointment) -> {
            // show detailed view
            try {
                if (map != null) {
                    final Stage dialog = new Stage();
                    dialog.initModality(Modality.NONE);
                    logEntryControl = new LogEntryControl();
                    logEntryControl.setLog(map.get(appointment));
                    Scene dialogScene = new Scene(logEntryControl, 300, 200);
                    dialog.setScene(dialogScene);
                    dialog.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });

        agenda.allowDraggingProperty().set(false);
        agenda.allowResizeProperty().set(false);
        // find the css file

        try {
            agenda.getStylesheets().add(this.getClass().getResource("/Agenda.css").toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        AnchorPane.setTopAnchor(agenda, 6.0);
        AnchorPane.setBottomAnchor(agenda, 6.0);
        AnchorPane.setLeftAnchor(agenda, 6.0);
        AnchorPane.setRightAnchor(agenda, 6.0);
        anchorpane.getChildren().add(agenda);

        initialize();

        return anchorpane;
    }

    private Job logbookSearchJob;

    private void initialize() {
        if (logbookSearchJob != null) {
            logbookSearchJob.cancel();
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put("search", "*");
        logbookSearchJob = LogbookSearchJob.submit(app.getClient(), map, logs -> Platform.runLater(() -> setLogs(logs)),
                (url, ex) -> ExceptionDetailsErrorDialog.openError("Logbook Search Error", ex.getMessage(), ex));

    }

    private void refresh() {
        map = new HashMap<Appointment, LogEntry>();
        map = this.logEntries.stream().collect(Collectors.toMap(new Function<LogEntry, Appointment>() {

            @Override
            public Appointment apply(LogEntry logentry) {
                // TODO Auto-generated method stub
                AppointmentImplLocal appointment = new Agenda.AppointmentImplLocal();
                appointment.withSummary(logentry.getDescription());
                appointment.withDescription(logentry.getDescription());
                appointment.withStartLocalDateTime(
                        LocalDateTime.ofInstant(logentry.getCreatedDate(), ZoneId.systemDefault()));
                appointment.withEndLocalDateTime(
                        LocalDateTime.ofInstant(logentry.getCreatedDate().plusSeconds(2400), ZoneId.systemDefault()));
                return appointment;
            }
        }, new Function<LogEntry, LogEntry>() {

            @Override
            public LogEntry apply(LogEntry logentry) {
                return logentry;
            }

        }));
        agenda.appointments().clear();
        agenda.appointments().setAll(map.keySet());

    }

    public void search() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("search", "*");
        super.search(map);
    }

    @Override
    public void setLogs(List<LogEntry> logs) {
        this.logEntries = logs;
        refresh();
    }

}
