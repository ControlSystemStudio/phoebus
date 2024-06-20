package org.phoebus.applications.uxanalytics.monitor;

import javafx.application.Platform;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.epics.vtype.Display;
import org.phoebus.ui.docking.DockItemWithInput;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Logger;


public class UXAToolkitListener implements ToolkitListener {

    Logger logger = Logger.getLogger(UXAToolkitListener.class.getName());

    private DisplayInfo targetDisplayInfo;

    Callable<DisplayInfo> notifyDisplayInfoCallable = new Callable<DisplayInfo>() {
        @Override
        public DisplayInfo call() throws Exception {
            // Replace this with the actual logic to get the DisplayInfo
            return targetDisplayInfo;
        }
    };

    FutureTask<DisplayInfo> notifyDisplayInfo = new FutureTask<>(notifyDisplayInfoCallable);

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
        DisplayInfo currentDisplayInfo = tabWrapper.getDisplayInfo();
        //We don't know a priori that a displayOpen action will succeed.
        //But, an 'open display' action will be followed by a call to 'loadDisplayFile'
        //in which, a runLater() will load whatever was requested. We can wait for this.
        //Once it has actually happened, then we can tell the backend about it.
        if(action.getType().equals(ActionInfo.ActionType.OPEN_DISPLAY)){
                new Thread(() -> {
                    try {
                    notifyDisplayInfo.get(1, TimeUnit.SECONDS);
                    System.out.println(currentDisplayInfo.getPath() + "->" + ((OpenDisplayActionInfo)action).getFile());
                    }
                    catch(TimeoutException t){
                        logger.info("DisplayInfo notification timed out, analytics stack ignoring this event.");
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }).start();
        }
        else{
            monitor.getPhoebusConnection().handleAction(tabWrapper, widget, action);
        }
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
        StackTraceElement[] stackTrace;
        if(user_args[0] instanceof DisplayInfo && user_args[1] instanceof StackTraceElement[]) {
            stackTrace = (StackTraceElement[]) user_args[1];
            for(StackTraceElement e: stackTrace){
                String methodName = e.getMethodName();
                if (methodName.equals("loadDisplayFile")) {
                    ResourceOpenSources source = getSourceOfOpen(stackTrace);
                    System.out.println("Method call: " + methodName);
                    System.out.println("Source: " + source);
                    targetDisplayInfo = (DisplayInfo) user_args[0];
                    notifyDisplayInfo.run();
                }
            }
        }
    }
}
