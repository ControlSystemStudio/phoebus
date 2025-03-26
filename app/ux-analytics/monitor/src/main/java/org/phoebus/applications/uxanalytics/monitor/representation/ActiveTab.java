package org.phoebus.applications.uxanalytics.monitor.representation;

import javafx.scene.Node;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.applications.uxanalytics.monitor.UXAMouseMonitor;
import org.phoebus.applications.uxanalytics.monitor.UXAToolkitListener;
import org.phoebus.ui.docking.DockItemWithInput;

import javafx.scene.input.MouseEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ActiveTab {

    private final ConcurrentLinkedDeque<Widget> widgets;
    private final DockItemWithInput parentTab;
    private final ToolkitListener toolkitListener;
    private final Supplier<Future<Boolean>> ok_to_close = () -> {
        this.detachListeners();
        return CompletableFuture.completedFuture(true);
    };
    private final Node jfxNode;
    private final UXAMouseMonitor mouseMonitor;
    private boolean listenersAdded = false;

    public ActiveTab(DockItemWithInput tab){
        widgets = new ConcurrentLinkedDeque<>();
        parentTab = tab;
        toolkitListener = new UXAToolkitListener();
        ((UXAToolkitListener)toolkitListener).setTabWrapper(this);
        jfxNode = tab.getContent();
        mouseMonitor = new UXAMouseMonitor(this);
        if(ActiveWindowsService.getInstance().isActive())
            this.addListeners();
        parentTab.addCloseCheck(ok_to_close);
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

}
