package org.phoebus.applications.uxanalytics.monitor;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitListener;


public class UXAToolkitListener implements ToolkitListener {

    private ActiveTab tabWrapper;
    private final UXAMonitor monitor = UXAMonitor.getInstance();
    void setTabWrapper(ActiveTab tabWrapper){
        this.tabWrapper = tabWrapper;
    }

    @Override
    public void handleAction(Widget widget, ActionInfo action) {
        System.out.println("Action");
        System.out.println(action.getType());
        System.out.println("Telling the monitor that this action emanated from " + tabWrapper.getParentTab());
        monitor.getPhoebusConnection().handleAction(tabWrapper, widget, action);
    }

    @Override
    public void handleWrite(Widget widget, Object value) {
        System.out.println("wrote from "+ widget+" from thread "+Thread.currentThread().getName());
        System.out.println("Telling backend that this PV was written from " + tabWrapper.getParentTab());
        monitor.getPhoebusConnection().handlePVWrite(tabWrapper, widget, widget.getPropertyValue("pv_name"), value);
    }

    @Override
    public void handleClick(Widget widget, boolean with_control) {
        //nothing for now
    }
}
