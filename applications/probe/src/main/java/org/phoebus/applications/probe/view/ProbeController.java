package org.phoebus.applications.probe.view;

import static org.diirt.datasource.ExpressionLanguage.channel;
import static org.diirt.util.time.TimeDuration.ofMillis;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.diirt.datasource.PVManager;
import org.diirt.datasource.PVReader;
import org.diirt.datasource.PVReaderEvent;
import org.diirt.datasource.PVReaderListener;
import org.diirt.vtype.Alarm;
import org.diirt.vtype.AlarmSeverity;
import org.diirt.vtype.SimpleValueFormat;
import org.diirt.vtype.Time;
import org.diirt.vtype.ValueFormat;
import org.diirt.vtype.ValueUtil;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionChangeListener;
import org.phoebus.framework.selection.SelectionService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class ProbeController {

    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
    private ValueFormat valueFormat = new SimpleValueFormat(3);

    @FXML
    Button btncalculate;
    @FXML
    TextField txtPVName;
    @FXML
    TextField txtValue;
    @FXML
    TextField txtTimeStamp;

    public String getPVName() {
        return txtPVName.getText();
    }

    public void setPVName(String pvName) {
        txtPVName.setText(pvName);
        search();
    }

    @FXML
    public void initialize() {
        // register selection listener
        SelectionService.getInstance().addListener(new SelectionChangeListener() {

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
        SelectionService.getInstance().setSelection(txtPVName, Arrays.asList(new ProcessVariable(txtPVName.getText())));
    }

    private PVReader<Object> pv;

    @FXML
    private void search() {

        // The PV is different, so disconnect and reset the visuals
        if (pv != null) {
            pv.close();
            pv = null;
        }

        // search for pv
        pv = PVManager.read(channel(txtPVName.getText())).readListener(new PVReaderListener<Object>() {
            @Override
            public void pvChanged(PVReaderEvent<Object> event) {
                final Object newValue = event.getPvReader().getValue();
                Platform.runLater(() -> {
                    if (event.getPvReader().lastException() != null) {
                        event.getPvReader().lastException().printStackTrace();
                    }
                    setValue(newValue, event.getPvReader().isConnected());
                    setTime(ValueUtil.timeOf(newValue));
                });
            }
        }).maxRate(ofMillis(100));

    }

    private void setTime(Time time) {
        if (time != null) {
            txtTimeStamp.setText(timeFormat
                    .format(Instant.ofEpochSecond(time.getTimestamp().getSec(), time.getTimestamp().getNanoSec())));
        } else {
            txtTimeStamp.setText(""); //$NON-NLS-1$
        }
    }

    private void setValue(Object value, boolean connection) {
        StringBuilder formattedValue = new StringBuilder();

        if (value != null) {
            String valueString = valueFormat.format(value);
            if (valueString != null) {
                formattedValue.append(valueString);
            }
        }

        appendAlarm(formattedValue, ValueUtil.alarmOf(value, connection));

        txtValue.setText(formattedValue.toString());
    }

    private void appendAlarm(StringBuilder builder, Alarm alarm) {
        if (alarm == null || alarm.getAlarmSeverity().equals(AlarmSeverity.NONE)) {
            return; // $NON-NLS-1$
        } else {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            builder.append('[').append(alarm.getAlarmSeverity()).append(" - ") //$NON-NLS-1$
                    .append(alarm.getAlarmName()).append(']');
        }
    }
}
