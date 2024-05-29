package org.phoebus.applications.uxanalytics.monitor;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitListener;


public class UXAToolkitListener implements ToolkitListener {

    @Override
    public void handleAction(Widget widget, ActionInfo action) {
        System.out.println("Action");
        System.out.println(action.getType());
    }

    @Override
    public void handleWrite(Widget widget, Object value) {
        System.out.println("wrote from "+ widget+" from thread "+Thread.currentThread().getName());
    }

    @Override
    public void handleClick(Widget widget, boolean with_control) {
        System.out.println("Clicky");
    }
}
