package org.phoebus.applications.probe.view;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.applications.probe.Messages;
import org.phoebus.applications.probe.Probe;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.util.time.TimestampFormats;

import io.reactivex.disposables.Disposable;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

@SuppressWarnings("nls")
public class ProbeController {

    private boolean editing = false;

    @FXML
    private TextField txtPVName;
    @FXML
    private TextField txtValue;
    @FXML
    private TextField txtAlarm;
    @FXML
    private TextField txtTimeStamp;
    @FXML
    private TextArea txtMetadata;

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

    private void setEditing(final boolean editing)
    {
        if (editing == this.editing)
            return;
        this.editing = editing;
        if (editing)
            txtValue.setStyle("-fx-control-inner-background: #FFFF00;");
        else
        {
            txtValue.setStyle("");
            // Restore current value
            // (which might soon be replaced by update from PV if we're writing)
            update(pv.read());
        }
    }

    @FXML
    public void initialize() {
        // Write entered value to PV
        txtValue.setOnKeyPressed(event ->
        {
            if (pv.isReadonly())
            {
                event.consume();
                return;
            }
            switch (event.getCode())
            {
            case ESCAPE:
                setEditing(false);
                break;
            case ENTER:
                final String entered = txtValue.getText();
                setEditing(false);
                // Write entered value to PV.
                // If PV accepts the value, it will send an update
                try
                {
                    pv.write(entered);
                }
                catch (Exception ex)
                {
                    Logger.getLogger(Probe.class.getPackageName())
                    .log(Level.WARNING, "Cannot write '" + entered + "' to PV " + pv.getName(), ex);
                }
                break;
            default:
                setEditing(true);
            }
        });
        txtValue.setOnMouseClicked(event ->
        {
            if (! pv.isReadonly())
                setEditing(true);
        });
        txtValue.focusedProperty().addListener((p, old, focus) ->
        {
            if (!focus)
                setEditing(false);
        });

        // Context menu to open other PV-aware tools
        final ContextMenu menu = new ContextMenu(new MenuItem());
        txtPVName.setOnContextMenuRequested(event ->
        {

            menu.getItems().clear();
            SelectionService.getInstance().setSelection("Probe", List.of(new ProcessVariable(txtPVName.getText().trim())));
            ContextMenuHelper.addSupportedEntries(txtPVName, menu);
            menu.show(txtPVName.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    private PV pv;
    private Disposable pv_flow, permission_flow;

    private void update(final VType value)
    {
        Platform.runLater(() -> setValue(value));
    }

    private void updateWritable(final Boolean writable)
    {
        Platform.runLater(() ->  txtValue.setEditable(writable));
    }

    @FXML
    private void search() {
        // The PV is different, so disconnect and reset the visuals
        if (pv != null)
        {
            if (permission_flow != null)
            {
                permission_flow.dispose();
                permission_flow = null;
            }
            if (pv_flow != null)
            {
                pv_flow.dispose();
                pv_flow = null;
            }
            PVPool.releasePV(pv);
            pv = null;
        }

        // search for pv, unless empty
        if (txtPVName.getText().isEmpty())
            return;

        SelectionService.getInstance().setSelection(txtPVName, Arrays.asList(new ProcessVariable(txtPVName.getText())));

        try
        {
            pv = PVPool.getPV(txtPVName.getText());
            pv_flow = pv.onValueEvent()
                        .throttleLatest(10, TimeUnit.MILLISECONDS)
                        .subscribe(this::update);
            permission_flow = pv.onAccessRightsEvent()
                    .throttleLatest(10, TimeUnit.MILLISECONDS)
                    .subscribe(this::updateWritable);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void setValue(final VType value) {
        if (editing)
            return;

        String valueString = null;
        if (value != null)
        {
            if (value instanceof VNumber)
                valueString = ((VNumber)value).getDisplay().getFormat().format(((VNumber)value).getValue().doubleValue());
            else if (value instanceof VString)
                valueString = ((VString)value).getValue();
            else if (value instanceof VEnum)
                valueString = ((VEnum)value).getValue();
            else
                valueString = Objects.toString(value);
        }
        if (valueString != null)
            txtValue.setText(valueString);
        else
            txtValue.setText("<null>");
        setTime(Time.timeOf(value));
        setAlarm(Alarm.alarmOf(value, value != null));
        setMetadata(value);
    }

    private void setTime(final Time time) {
        if (time != null) {
            txtTimeStamp.setText(TimestampFormats.FULL_FORMAT.format(time.getTimestamp()));
        } else {
            txtTimeStamp.setText(""); //$NON-NLS-1$
        }
    }

    private void setAlarm(final Alarm alarm)
    {
        if (alarm == null  ||  alarm.getSeverity() == AlarmSeverity.NONE)
            txtAlarm.setText("");
        else
        {
            final Color col = SeverityColors.getTextColor(alarm.getSeverity());
            txtAlarm.setStyle("-fx-text-fill: rgba(" + (int)(col.getRed()*255) + ',' +
                                                       (int)(col.getGreen()*255) + ',' +
                                                       (int)(col.getBlue()*255) + ',' +
                                                             col.getOpacity()*255 + ");");
            txtAlarm.setText(alarm.getSeverity() + " - " + alarm.getName());
        }
    }

    private void setMetadata(final VType value)
    {
        final StringBuilder buf = new StringBuilder();
        if (value instanceof VEnum)
        {
            final VEnum eval = (VEnum) value;
            buf.append(Messages.EnumLbls).append("\n");
            int i = 0;
            for (String label : eval.getDisplay().getChoices())
                buf.append(i++).append(" = ").append(label).append("\n");
        }
        else if (value instanceof VNumber)
        {
            final Display dis = ((VNumber) value).getDisplay();
            buf.append(Messages.Units).append(dis.getUnit()).append("\n");
            buf.append(Messages.Format).append(dis.getFormat().format(0.123456789)).append("\n");
            buf.append(Messages.Range).append(dis.getControlRange().getMinimum()).append(" .. ").append(dis.getControlRange().getMaximum()).append("\n");
            buf.append(Messages.Warnings).append(dis.getWarningRange().getMinimum()).append(" .. ").append(dis.getWarningRange().getMaximum()).append("\n");
            buf.append(Messages.Alarms).append(dis.getAlarmRange().getMinimum()).append(" .. ").append(dis.getAlarmRange().getMaximum()).append("\n");
        }

        txtMetadata.setText(buf.toString());
    }
}
