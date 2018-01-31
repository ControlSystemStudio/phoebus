package org.csstudio.trends.databrowser3.ui.plot;

import org.csstudio.trends.databrowser3.Messages;
import org.phoebus.ui.time.TemporalAmountPane;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class StartEndDialog extends Dialog<TimeRelativeInterval>
{
    private final TimeRelativeIntervalPane times = new TimeRelativeIntervalPane(TemporalAmountPane.Type.ONLY_NOW);

    public StartEndDialog()
    {
        setTitle(Messages.TimeColumn);

        getDialogPane().setContent(times);


        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(button ->
        {
            if (button == ButtonType.OK)
                return times.getTimeRelativeInterval();
            return null;
        });
    }
}
