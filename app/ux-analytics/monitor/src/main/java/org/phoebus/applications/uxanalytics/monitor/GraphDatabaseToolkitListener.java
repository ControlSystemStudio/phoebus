package org.phoebus.applications.uxanalytics.monitor;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitListener;

public class GraphDatabaseToolkitListener implements ToolkitListener {

    @Override
    public void handleAction(Widget widget, ActionInfo action) {
        System.out.println("Action");
    }

    @Override
    public void handleWrite(Widget widget, Object value) {
        System.out.println("wrote");
    }

    @Override
    public void handleClick(Widget widget, boolean with_control) {
        System.out.println("Clicky");
    }
}
