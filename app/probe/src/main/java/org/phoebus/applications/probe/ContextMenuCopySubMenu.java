package org.phoebus.applications.probe;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.ui.spi.ContextMenuEntry;

public class ContextMenuCopySubMenu implements ContextMenuEntry {
    @Override
    public String getName() {
        return Messages.CopySubMenu;
    }

    @Override
    public Class<?> getSupportedType() {
        return ProcessVariable.class;
    }
}
