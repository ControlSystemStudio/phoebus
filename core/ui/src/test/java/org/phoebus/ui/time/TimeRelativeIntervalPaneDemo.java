/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.time;

import java.time.Duration;

import org.phoebus.ui.javafx.ApplicationWrapper;
import org.phoebus.ui.time.TemporalAmountPane.Type;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/** Demo of {@link TimeRelativeIntervalPane}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeRelativeIntervalPaneDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final TimeRelativeIntervalPane ui = new TimeRelativeIntervalPane(Type.TEMPORAL_AMOUNTS_AND_NOW);

        ui.setInterval(TimeRelativeInterval.of(Duration.ofDays(1).plus(Duration.ofHours(6)),
                                               Duration.ZERO));

        final Button test = new Button("Test");
        test.setOnAction(event ->
        {
            final TimeRelativeInterval interval = ui.getInterval();
            System.out.println("Interval: " + interval);
            System.out.println("Right now that means " + interval.toAbsoluteInterval());
        });

        final GridPane layout = new GridPane();
        layout.add(ui, 0, 0);
        layout.add(test, 0, 1);
        GridPane.setHalignment(test, HPos.RIGHT);
        stage.setScene(new Scene(layout, 600, 500));
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(TimeRelativeIntervalPaneDemo.class, args);
    }
}
