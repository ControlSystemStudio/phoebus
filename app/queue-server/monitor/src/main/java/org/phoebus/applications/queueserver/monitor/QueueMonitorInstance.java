package org.phoebus.applications.queueserver.monitor;

import javafx.scene.Node;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

final class QueueMonitorInstance implements AppInstance {

    private final AppDescriptor desc;
    private final Node          view;

    QueueMonitorInstance(AppDescriptor desc, Node view) {
        this.desc = desc;
        this.view = view;
    }

    @Override public AppDescriptor getAppDescriptor() { return desc; }
    public Node create() { return view; }

    @Override public void restore(Memento m) { /* nothing */ }
    @Override public void save   (Memento m) { /* nothing */ }
}
