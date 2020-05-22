package org.phoebus.applications.saveandrestore.ui;

import org.phoebus.framework.persistence.Memento;

public interface ISaveAndRestoreController {
    void save(final Memento memento);
    void restore(final Memento memento);
}
