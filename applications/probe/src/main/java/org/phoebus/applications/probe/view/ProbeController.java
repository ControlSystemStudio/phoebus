package org.phoebus.applications.probe.view;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionChangeListener;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.util.time.TimestampFormats;
import org.phoebus.vtype.Alarm;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.SimpleValueFormat;
import org.phoebus.vtype.Time;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFormat;
import org.phoebus.vtype.ValueUtil;

import io.reactivex.disposables.Disposable;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class ProbeController {

    private ValueFormat valueFormat = new SimpleValueFormat(3);

    @FXML
    Button btncalculate;
    @FXML
    TextField txtPVName;
    @FXML
    TextField txtValue;
    @FXML
    TextField txtTimeStamp;

    public TextField getPVField() {
        return txtPVName;
    }

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

    private PV pv;
    private Disposable pv_flow;

    private void update(final VType value)
    {
        Platform.runLater(() ->
        {
            setValue(value);
            setTime(ValueUtil.timeOf(value));
        });
    }

    @FXML
    private void search() {
        // The PV is different, so disconnect and reset the visuals
        if (pv != null) {
            pv_flow.dispose();
            PVPool.releasePV(pv);
            pv = null;
        }

        // search for pv, unless empty
        if (txtPVName.getText().isEmpty())
            return;
        try
        {
            pv = PVPool.getPV(txtPVName.getText());
            pv_flow = pv.onValueEvent()
                        .throttleLast(10, TimeUnit.MILLISECONDS)
                        .subscribe(this::update);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void setTime(Time time) {
        if (time != null) {
            txtTimeStamp.setText(TimestampFormats.FULL_FORMAT.format(time.getTimestamp()));
        } else {
            txtTimeStamp.setText(""); //$NON-NLS-1$
        }
    }

    private void setValue(VType value) {
        StringBuilder formattedValue = new StringBuilder();

        if (value != null) {
            String valueString = valueFormat.format(value);
            if (valueString != null) {
                formattedValue.append(valueString);
            }
        }

        appendAlarm(formattedValue, ValueUtil.alarmOf(value, value != null));

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
