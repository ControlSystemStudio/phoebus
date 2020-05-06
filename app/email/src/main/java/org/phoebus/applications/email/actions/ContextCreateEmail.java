package org.phoebus.applications.email.actions;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.ContextMenuEntry;

/**
 * A prototype action entry for starting the email application
 *
 * @author Kunal Shroff
 *
 */
public class ContextCreateEmail implements ContextMenuEntry {

    private static final Class<?> supportedType = String.class;

    @Override
    public String getName() {
        return EmailApp.DISPLAY_NAME;
    }

    @Override
    public void callWithSelection(Selection selection) {
        ApplicationService.createInstance(EmailApp.NAME);
    }

    @Override
    public Class<?> getSupportedType() {
        return supportedType;
    }
}
