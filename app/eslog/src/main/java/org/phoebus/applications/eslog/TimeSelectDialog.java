package org.phoebus.applications.eslog;

import org.phoebus.ui.time.TemporalAmountPane.Type;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class TimeSelectDialog extends Dialog<TimeRelativeInterval>
{
    final TimeRelativeIntervalPane intervalPane;

    public TimeSelectDialog(TimeRelativeInterval interval)
    {
        setTitle("Select Time");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK,
                ButtonType.CANCEL);

        intervalPane = new TimeRelativeIntervalPane(
                Type.TEMPORAL_AMOUNTS_AND_NOW);
        intervalPane.setInterval(interval);
        getDialogPane().setContent(intervalPane);

        setResultConverter(this::buttonPressed);
    }

    protected TimeRelativeInterval buttonPressed(ButtonType button)
    {
        if (button != ButtonType.OK)
        {
            return null;
        }
        return intervalPane.getInterval();
    }
}
