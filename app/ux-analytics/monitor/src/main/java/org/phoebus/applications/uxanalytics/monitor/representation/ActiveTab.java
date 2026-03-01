package org.phoebus.applications.uxanalytics.monitor.representation;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.Window;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.applications.uxanalytics.monitor.UXAMouseMonitor;
import org.phoebus.applications.uxanalytics.monitor.UXAToolkitListener;
import org.phoebus.applications.uxanalytics.monitor.util.FileUtils;
import org.phoebus.ui.docking.DockItemWithInput;

import javafx.scene.input.MouseEvent;
import org.phoebus.ui.docking.DockStage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ActiveTab {

    private final ConcurrentLinkedDeque<Widget> widgets;
    private final DockItemWithInput parentTab;
    private final String parentWindowID;
    private final ToolkitListener toolkitListener;
    private final Node jfxNode;
    private final UXAMouseMonitor mouseMonitor;
    private boolean listenersAdded = false;
    private String analyticsName;

    public ActiveTab(DockItemWithInput tab, ActiveWindowsService activeWindowsService, String windowID){
        widgets = new ConcurrentLinkedDeque<>();
        parentTab = tab;
        toolkitListener = new UXAToolkitListener();
        ((UXAToolkitListener)toolkitListener).setTabWrapper(this);
        jfxNode = tab.getContent();
        mouseMonitor = new UXAMouseMonitor(this);
        parentWindowID = windowID;
        if(activeWindowsService.isActive())
            this.addListeners();
        if (windowID != null && !windowID.isBlank()){
            final Supplier<Future<Boolean>> ok_to_close = () -> {
                this.detachListeners();
                ActiveWindowsService.getInstance().getActiveWindowsAndTabs().get(parentWindowID).remove(this);
                return CompletableFuture.completedFuture(true);
            };
            parentTab.addCloseCheck(ok_to_close);
        }
    }

    public ActiveTab(DockItemWithInput tab, String windowID){
        this(tab, ActiveWindowsService.getInstance(), windowID);
    }

    public ToolkitListener getToolkitListener(){
        return toolkitListener;
    }

    public DisplayInfo getDisplayInfo(){
        DisplayRuntimeInstance instance = parentTab.getApplication();
        return instance.getDisplayInfo();
    }

    public synchronized void add(Widget widget){
        widgets.add(widget);
    }

    public synchronized void remove(Widget widget){
        widgets.remove(widget);
    }

    public synchronized void detachListeners(){
        if(!listenersAdded) return;
        DisplayRuntimeInstance instance = (DisplayRuntimeInstance) parentTab.getApplication();
        if(instance != null) {
            instance.removeListener(toolkitListener);
        }
        if(jfxNode != null) {
            jfxNode.removeEventFilter(MouseEvent.MOUSE_CLICKED, mouseMonitor);
        }
        listenersAdded = false;
    }

    public synchronized void addListeners(){
        if (listenersAdded) return;
        DisplayRuntimeInstance instance = (DisplayRuntimeInstance) parentTab.getApplication();
        if(instance != null)
            instance.addListener(toolkitListener);
        if(jfxNode != null)
            jfxNode.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseMonitor);
        listenersAdded = true;
    }

    public boolean isListening(){
        return listenersAdded;
    }

    public DockItemWithInput getParentTab() {
        return parentTab;
    }

    public JFXRepresentation getJFXRepresentation() {return ((DisplayRuntimeInstance)parentTab.getApplication()).getRepresentation();}

    public double getZoom(){
        return getJFXRepresentation().getZoom();
    }

    public double getHeight(){
        return getJFXRepresentation().getModelRoot().getHeight();
    }

    public double getWidth(){
        return getJFXRepresentation().getModelRoot().getWidth();
    }

    public String getAnalyticsName(){
        if(analyticsName == null){
            analyticsName = FileUtils.analyticsPathForTab(this);
        }
        return analyticsName;
    }

    public UXAMouseMonitor getMouseMonitor() {
        return mouseMonitor;
    }

    @Override
    public String toString() {
        return (String)parentTab.getProperties().get(DockStage.KEY_ID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof String){
            return this.toString().equals(obj);
        }
        else if (obj instanceof ActiveTab){
            return this.toString().equals(((ActiveTab) obj).toString());
        }
        else if (obj instanceof DockItemWithInput){
            return this.toString().equals(((DockItemWithInput) obj).getProperties().get(DockStage.KEY_ID));
        }
        return false;
    }

    public String getParentWindowID() {
        return parentWindowID;
    }
}
