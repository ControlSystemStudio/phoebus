package org.phoebus.applications.uxanalytics.monitor;

import javafx.application.Platform;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.epics.vtype.Display;
import org.phoebus.ui.docking.DockItemWithInput;

import javax.lang.model.type.ArrayType;
import java.io.File;
import java.io.InputStream;
import java.util.*;
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

    //Traverse down a given call stack to find out what caused the display to open
    private static ResourceOpenSources getSourceOfOpen(StackTraceElement[] stackTrace){
        for(StackTraceElement e: stackTrace){
            String methodName =  unmangleLambda(e.getMethodName());
            String fullName = e.getClassName()+"."+methodName;
            if(openSources.containsKey(fullName)){
                return openSources.get(fullName);
            }
        }
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
        StackTraceElement[] stackTrace;
        if(user_args[0] instanceof List &&
                ((List) user_args[0]).get(0) instanceof DisplayInfo &&
                user_args[1] instanceof StackTraceElement[]) {
            List<DisplayInfo> dst_src = (List<DisplayInfo>) user_args[0];
            stackTrace = (StackTraceElement[]) user_args[1];
            for(StackTraceElement e: stackTrace){
                String methodName = e.getMethodName();
                if (methodName.equals("loadDisplayFile")) {
                    ResourceOpenSources source = getSourceOfOpen(stackTrace);
                    switch(source){
                        case ACTION_BUTTON:
                            //nothing, handled by handleAction
                            break;

                        case NAVIGATION_BUTTON:
                        case RELOAD:
                            monitor.getPhoebusConnection().handleDisplayOpen(dst_src.get(0), dst_src.get(1), source);
                            break;

                        case FILE_BROWSER:
                        case RESTORED:
                        case TOP_RESOURCES:
                        case UNKNOWN:
                            monitor.getPhoebusConnection().handleDisplayOpen(dst_src.get(0), null, source);
                    }
                }
            }
        }
    }
}

