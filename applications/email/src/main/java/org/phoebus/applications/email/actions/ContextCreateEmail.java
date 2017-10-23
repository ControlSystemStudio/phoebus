package org.phoebus.applications.email.actions;

import java.util.Arrays;
import java.util.List;

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

    private static final List<Class> supportedTypes = Arrays.asList(String.class);

    @Override
    public String getName() {
        return EmailApp.DISPLAY_NAME;
    }

    @Override
    public Object callWithSelection(Selection selection) {
        ApplicationService.findApplication(EmailApp.NAME).create();
        return null;
    }

    @Override
    public List<Class> getSupportedTypes() {
        return supportedTypes;
    }

}
