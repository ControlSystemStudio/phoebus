/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.stage.Stage;
import org.epics.util.array.ArrayBoolean;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VIntArray;
import org.phoebus.pv.PVFactory;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.javafx.ApplicationWrapper;

public class ComparisonDialogDemo extends ApplicationWrapper {

    public static void main(String[] args){
        launch(ComparisonDialogDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        VDoubleArray vDoubleArray =
                VDoubleArray.of(ArrayDouble.of(1, 2,3, 4, 5),
                        Alarm.none(),
                        Time.now(), Display.none());
        ComparisonDialog comparisonDialog =
                new ComparisonDialog(vDoubleArray, "loc://x(1, 8, 7, 0, -1)");
        comparisonDialog.show();
    }
}
