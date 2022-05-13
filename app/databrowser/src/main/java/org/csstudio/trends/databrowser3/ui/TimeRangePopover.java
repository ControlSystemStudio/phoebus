package org.csstudio.trends.databrowser3.ui;

import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.time.TemporalAmountPane;
import org.phoebus.ui.time.TimeRelativeIntervalPane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/** Time range pop-over */
public class TimeRangePopover extends PopOver {

    private static final Logger log = Logger.getLogger(TimeRangePopover.class.getName());

    /** @param model Model
     *  @param cancelCallback Callback when cancelled
     *  @param applyCallback Callback to apply
     *  @return Popover
     */
    public static TimeRangePopover withDefaultTimePane(final Model model, BiConsumer<TimeRelativeIntervalPane, PopOver> cancelCallback, BiConsumer<TimeRelativeIntervalPane, PopOver> applyCallback) {
        return new TimeRangePopover(new TimeRelativeIntervalPane(TemporalAmountPane.Type.ONLY_NOW), model, cancelCallback, applyCallback);
    }

    /** @param timePane Time pane
     *  @param model Model
     *  @param cancelCallback Callback when cancelled
     *  @param applyCallback Callback to apply
     */
    public TimeRangePopover(final TimeRelativeIntervalPane timePane, final Model model, BiConsumer<TimeRelativeIntervalPane, PopOver> cancelCallback, BiConsumer<TimeRelativeIntervalPane, PopOver> applyCallback) {
        // Initialize the PopOver with the timepane as the content
        super(timePane);

        // Set the interval for the TimePane
        timePane.setInterval(model.getTimerange());

        // Add Apply and Close buttons to the TimePane
        Button cancelButton = new Button();
        cancelButton.setText("Close");
        Button applyButton = new Button();
        applyButton.setText("Apply");

        HBox buttonBar = new HBox();
        buttonBar.setSpacing(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(5));
        buttonBar.getChildren().add(cancelButton);
        buttonBar.getChildren().add(applyButton);
        timePane.add(buttonBar, 0, 5, 4, 1);

        cancelButton.setOnAction(actionEvent -> {
            try {
                cancelCallback.accept(timePane, this);
            } catch (Exception e) {
                log.log(Level.WARNING, "Could not execute cancel action on TimeRangePopover", e);
            }
        });
        applyButton.setOnAction(actionEvent -> {
            try {
                applyCallback.accept(timePane, this);
            } catch (Exception e) {
                log.log(Level.WARNING, "Could not execute apply action on TimeRangePopover", e);
            }
        });
    }

}
