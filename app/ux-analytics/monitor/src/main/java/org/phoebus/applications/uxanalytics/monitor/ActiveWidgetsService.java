package org.phoebus.applications.uxanalytics.monitor;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.ui.docking.DockItemWithInput;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ActiveWidgetsService {

    private final ConcurrentLinkedDeque<Widget> widgets;
    private final DockItemWithInput parentTab;
    private final ToolkitListener listener;
    private final Supplier<Future<Boolean>> ok_to_close = () -> {
        this.close();
        return null;
    };

    public ActiveWidgetsService(DockItemWithInput tab){
        widgets = new ConcurrentLinkedDeque<>();
        parentTab = tab;
        listener = new GraphDatabaseToolkitListener();
        ((DisplayRuntimeInstance)tab.getProperties().get("application")).addListener(listener);
        parentTab.addCloseCheck(ok_to_close);
    }

    public ToolkitListener getListener(){
        return listener;
    }

    public synchronized void add(Widget widget){
        widgets.add(widget);
    }

    public synchronized void remove(Widget widget){
        widgets.remove(widget);
    }

    public synchronized void close(){
        ((DisplayRuntimeInstance)parentTab.getProperties().get("application")).removeListener(listener);
    }
}
