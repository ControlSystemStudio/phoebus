package org.phoebus.applications.email.actions;

import java.util.Arrays;
import java.util.List;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.ContextMenuEntry;
import org.phoebus.framework.workbench.ApplicationService;

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
    public Object getIcon() {
        return null;
    }

    @Override
    public List<Class> getSupportedTypes() {
        return supportedTypes;
    }

}
