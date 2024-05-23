package org.phoebus.applications.uxanalytics.monitor;

import javafx.scene.Node;
import javafx.stage.Window;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.docking.DockItemWithInput;


public abstract class UXWidgetWrapper {

    Node jfxNode;
    Window window;
    Widget widget;
    DockItemWithInput parentTab;

    public abstract void createListener();
}
