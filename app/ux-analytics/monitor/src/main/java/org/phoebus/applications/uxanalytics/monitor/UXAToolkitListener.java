package org.phoebus.applications.uxanalytics.monitor;

import javafx.application.Platform;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.phoebus.ui.docking.DockItemWithInput;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Logger;


public class UXAToolkitListener implements ToolkitListener {

    Logger logger = Logger.getLogger(UXAToolkitListener.class.getName());

    public static final HashMap<String, ResourceOpenSources> openSources = new HashMap<>(
            Map.of(
                    "org.csstudio.display.builder.runtime.ActionUtil.openDisplay", ResourceOpenSources.ACTION_BUTTON,
                    "org.phoebus.ui.application.PhoebusApplication.fileOpen", ResourceOpenSources.FILE_BROWSER,
                    "org.csstudio.display.builder.runtime.app.NavigationAction.navigate", ResourceOpenSources.NAVIGATION_BUTTON,
                    "org.phoebus.ui.internal.MementoHelper.restoreDockItem", ResourceOpenSources.RESTORED,
                    "org.phoebus.ui.application.PhoebusApplication.createTopResourcesMenu", ResourceOpenSources.TOP_RESOURCES,
                    "org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance.reload", ResourceOpenSources.RELOAD
            )
    );

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

    //Traverse down the call stack to find out what caused the display to open
    private static ResourceOpenSources getSourceOfOpen(StackTraceElement[] stackTrace){
        for(StackTraceElement e: stackTrace){
            String methodName =  unmangleLambda(e.getMethodName());
            String fullName = e.getClassName()+"."+methodName;
            if(openSources.containsKey(fullName)){
                return openSources.get(fullName);
            }
        }
        /*for(StackTraceElement e: stackTrace){
            String methodName =  unmangleLambda(e.getMethodName());
            System.out.println(e.getClassName()+"."+methodName);
        }*/
        return ResourceOpenSources.UNKNOWN;
    }

    private static String unmangleLambda(String expression){
        //find index of first '$' after 'lambda$'
        if(expression.contains("lambda$")) {
            int start = expression.indexOf("lambda$") + 7;
            int end = expression.indexOf("$", start);
            return expression.substring(start, end);
        }
        return expression;
    }

    @Override
    public void handleMethodCalled(Object... user_args) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        //zeroth element is getStackTrace itself, first is this method,
        //second is fireMethodCalled, third is the one we care about
        String methodName = stackTrace[3].getMethodName();
        if(methodName.equals("loadDisplayFile")){
            ResourceOpenSources source = getSourceOfOpen(stackTrace);
            System.out.println("Method call: "+methodName);
            System.out.println("Source: "+source);
        }
    }
}
