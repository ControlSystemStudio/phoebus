package org.phoebus.ui.application;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.dialog.ListPickerDialog;

import javafx.scene.control.Dialog;
import javafx.stage.Stage;

/**
 * A Service to help launch a particular application.
 *
 * The service allows you to directly launch an application if you have the
 * application name.
 *
 * It also queries the {@link ResourceHandlerService} and
 * {@link ApplicationService} to provide the option to launch application from
 * their associated resources.
 *
 * @author Kunal Shroff
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ApplicationLauncherService {

    /** Logger for all application messages */
    public static final Logger logger = Logger.getLogger(ApplicationLauncherService.class.getName());

    /**
     * Launch application
     *
     * @param appName name of the application to be launched
     */
    public static void launchApp(final String appName) {
        final AppDescriptor app = ApplicationService.findApplication(appName);
        if (app == null)
        {
            logger.log(Level.SEVERE, "Unknown application '" + appName + "'");
            return;
        }
        app.create();
    }

    /**
     * @param resource Resource received as command line argument
     * @param prompt Prompt if there are multiple applications, or use first one?
     * @param stage If prompt is enabled, a selection dialog will be launched
     *              positioned next to the provided stage. If <code>null</code> then the
     *              default or first application will be used
     * @return <code>true</code> if resource could be opened
     */
    public static boolean openResource(final URI resource, final boolean prompt, final Stage stage)
    {
        final AppResourceDescriptor application = findApplication(resource, prompt, stage);
        if (application == null)
            return false;
        logger.log(Level.INFO, "Opening " + resource + " with " + application.getName());
        application.create(resource);
        return true;
    }

    /**
     * Open file
     *
     * @param stage
     *            Parent stage
     * @param prompt
     *            Prompt for application (if there are multiple options), or use
     *            default app?
     * @param stage
     *            If prompt is enabled, a selection dialog will be launched
     *            positioned next to the provided stage. If null then the
     *            default or first application will be used
     * @return <code>true</code> if file could be opened
     */
    public static boolean openFile(File file, final boolean prompt, final Stage stage) {
        return openResource(ResourceParser.getURI(file), prompt, stage);
    }

    /**
     * Locate suitable application
     *
     * @param resource
     *            Resource {@link URI}
     * @param prompt
     *            Prompt if there are multiple applications, or use first one?
     * @param stage
     *            If prompt is enabled, a selection dialog will be launched
     *            positioned next to the provided stage. If <code>null</code> then the
     *            default or first application will be used
     * @return Application for opening resource, or <code>null</code> if none
     *         found
     */
    public static AppResourceDescriptor findApplication(final URI resource, final boolean prompt, final Stage stage)
    {
        // Does resource request a specific application?
        final String app_name = ResourceParser.getAppName(resource);
        if (app_name != null)
        {
            final AppDescriptor app = ApplicationService.findApplication(app_name);
            if (app == null)
            {
                logger.log(Level.WARNING, "Unknown application '" + app_name + "'");
                return null;
            }
            if (app instanceof AppResourceDescriptor)
                return (AppResourceDescriptor) app;
            else
            {
                logger.log(Level.WARNING, "'" + app_name + "' application does not handle resources");
                return null;
            }
        }

        // Check all applications
        final List<AppResourceDescriptor> applications = ApplicationService.getApplications(resource);
        if (applications.isEmpty())
        {
            logger.log(Level.INFO, "No application found for opening " + resource);
            return null;
        }

        // Only one app?
        if (applications.size() == 1)
            return applications.get(0);

        // Pick default application based on preference setting?
        if (!prompt || stage == null)
        {
            for (AppResourceDescriptor app : applications)
                for (String part : Preferences.default_apps)
                    if (app.getName().contains(part))
                        return app;
            // , not just the first one, which may be undefined
            logger.log(Level.WARNING, "No default application found for opening " + resource + ", using first one");
            return applications.get(0);
        }

        // Prompt user which application to use for this resource
        final List<String> options = applications.stream().map(app -> app.getDisplayName()).collect(Collectors.toList());
        final Dialog<String> which = new ListPickerDialog(stage.getScene().getRoot(), options, null);
        which.setTitle(Messages.OpenTitle);
        which.setHeaderText(Messages.OpenHdr + resource);
        which.setWidth(300);
        which.setHeight(300);
        final Optional<String> result = which.showAndWait();
        if (! result.isPresent())
            return null;
        return applications.get(options.indexOf(result.get()));
    }

}
