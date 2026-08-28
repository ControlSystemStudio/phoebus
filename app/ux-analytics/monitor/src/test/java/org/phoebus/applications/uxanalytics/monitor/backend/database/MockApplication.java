package org.phoebus.applications.uxanalytics.monitor.backend.database;

import javafx.application.Application;
import javafx.stage.Stage;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXStageRepresentation;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeApplication;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.testfx.framework.junit.ApplicationTest;

public class MockApplication extends ApplicationTest {

    private DisplayRuntimeApplication app;
    private DockItemWithInput dockItem;



    @Override
    public void start(Stage stage) throws Exception {
        String display_path = ServiceLayerConnectionTest.class.getResource("/test.bob").getPath().replace("file:", "");
        //final DockItemWithInput
    }
}
