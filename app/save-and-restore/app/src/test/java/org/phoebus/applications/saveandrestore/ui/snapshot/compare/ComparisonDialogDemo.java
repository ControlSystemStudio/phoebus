/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.stage.Stage;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDoubleArray;
import org.phoebus.ui.javafx.ApplicationWrapper;

public class ComparisonDialogDemo extends ApplicationWrapper {

    public static void main(String[] args){
        launch(ComparisonDialogDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        VDoubleArray vDoubleArray =
                VDoubleArray.of(ArrayDouble.of(5.0, 6.0, 7.0, 8.0, 9.0),
                        Alarm.none(),
                        Time.now(),
                        Display.none());
        ComparisonDialog comparisonDialog =
                new ComparisonDialog(vDoubleArray, "MUSIGNY");
        comparisonDialog.show();
    }
}
