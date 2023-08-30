package org.phoebus.product;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.application.ApplicationServer;
import org.phoebus.ui.application.PhoebusApplication;

import javafx.application.Application;

@SuppressWarnings("nls")
public class Launcher
{
    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(Launcher.class.getResourceAsStream("/logging.properties"));
        final Logger logger = Logger.getLogger(Launcher.class.getName());

        // Can't change default charset, but warn if it's not UTF-8.
        // Config files for displays, data browser etc. explicitly use XMLUtil.ENCODING = "UTF-8".
        // EPICS database files, strings in Channel Access or PVAccess are expected to use UTF-8.
        // New Java API like java.nio.file.Files defaults to UTF-8,
        // but library code including JCA simply calls new String(byte[]).
        // The underlying Charset.defaultCharset() checks "file.encoding",
        // but this happens at an early stage of VM startup.
        // Calling System.setPropertu("file.encoding", "UTF-8") in main() is already too late,
        // must add -D"file.encoding=UTF-8" to java start up or JAVA_TOOL_OPTIONS.
        final Charset cs = Charset.defaultCharset();
        if (! "UTF-8".equalsIgnoreCase(cs.displayName()))
        {
            logger.severe("Default charset is " + cs.displayName() + " instead of UTF-8.");
            logger.severe("Add    -D\"file.encoding=UTF-8\"    to java command line or JAVA_TOOL_OPTIONS");
        }
        Locations.initialize();
        // Check for site-specific settings.ini bundled into distribution
        // before potentially adding command-line settings.
        final File site_settings = new File(Locations.install(), "settings.ini");
        if (site_settings.canRead())
        {
            logger.log(Level.CONFIG, "Loading settings from " + site_settings);
            PropertyPreferenceLoader.load(new FileInputStream(site_settings));
        }

        // Handle arguments, potentially not even starting the UI
        final List<String> args = new ArrayList<>(List.of(original_args));
        final Iterator<String> iter = args.iterator();
        int port = -1;
        try
        {
            while (iter.hasNext())
            {
                final String cmd = iter.next();
                if (cmd.startsWith("-h"))
                {
                    help();
                    return;
                }

                if (cmd.equals("-splash"))
                {
                    iter.remove();
                    Preferences.userNodeForPackage(org.phoebus.ui.Preferences.class)
                               .putBoolean(org.phoebus.ui.Preferences.SPLASH, true);
                }
                else if (cmd.equals("-nosplash"))
                {
                    iter.remove();
                    Preferences.userNodeForPackage(org.phoebus.ui.Preferences.class)
                               .putBoolean(org.phoebus.ui.Preferences.SPLASH, false);
                }
                else if (cmd.equals("-logging"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -logging file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    LogManager.getLogManager().readConfiguration(new FileInputStream(filename));
                }
                else if (cmd.equals("-settings"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -settings file name");
                    iter.remove();
                    final String location = iter.next();
                    iter.remove();

                    logger.info("Loading settings from " + location);
                    if (location.endsWith(".xml"))
                        Preferences.importPreferences(new FileInputStream(location));
                    else
                        PropertyPreferenceLoader.load(location);
                  
                }
                else if (cmd.equals("-export_settings"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -export_settings file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    System.out.println("Exporting settings to " + filename);
                    Preferences.userRoot().node("org/phoebus").exportSubtree(new FileOutputStream(filename));
                    return;
                }
                else if (cmd.equals("-server"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -server port");
                    iter.remove();
                    port = Integer.parseInt(iter.next());
                    iter.remove();
                }
                else if (cmd.equals("-list"))
                {
                    iter.remove();
                    final Collection<AppDescriptor> apps = ApplicationService.getApplications();
                    System.out.format("Name                 Description          File Extensions\n");
                    for (AppDescriptor app : apps)
                    {
                        if (app instanceof AppResourceDescriptor)
                        {
                            final AppResourceDescriptor app_res = (AppResourceDescriptor) app;
                            System.out.format("%-20s %-20s %s\n",
                                              "'" + app.getName() + "'",
                                              app.getDisplayName(),
                                              app_res.supportedFileExtentions().stream().collect(Collectors.joining(", ")));
                        }
                        else
                            System.out.format("%-20s %s\n", "'" + app.getName() + "'", app.getDisplayName());
                    }
                    return;
                }
                else if (cmd.equals("-main"))
                {
                    iter.remove();
                    if (! iter.hasNext())
                        throw new Exception("Missing -main name");
                    final String main = iter.next();
                    iter.remove();

                    // Locate Main class and its main()
                    final Class<?> main_class = Class.forName(main);
                    final Method main_method = main_class.getDeclaredMethod("main", String[].class);
                    // Collect remaining arguments
                    final List<String> new_args = new ArrayList<>();
                    iter.forEachRemaining(new_args::add);
                    main_method.invoke(null, new Object[] { new_args.toArray(new String[new_args.size()]) });
                    return;
                }
            }
        }
        catch (Exception ex)
        {
            help();
            System.out.println();
            ex.printStackTrace();
            return;
        }

        logger.info("Phoebus (PID " + ProcessHandle.current().pid() + ")");

        // Check for an existing instance
        // If found, pass remaining arguments to it,
        // instead of starting a new application
        if (port > 0)
        {
            final ApplicationServer server = ApplicationServer.create(port);
            if (! server.isServer())
            {
                server.sendArguments(args);
                return;
            }
        }

        // Remaining args passed on
        Application.launch(PhoebusApplication.class, args.toArray(new String[args.size()]));
    }

    private static void help()
    {
        System.out.println(" _______           _______  _______  ______            _______ ");
        System.out.println("(  ____ )|\\     /|(  ___  )(  ____ \\(  ___ \\ |\\     /|(  ____ \\");
        System.out.println("| (    )|| )   ( || (   ) || (    \\/| (   ) )| )   ( || (    \\/");
        System.out.println("| (____)|| (___) || |   | || (__    | (__/ / | |   | || (_____ ");
        System.out.println("|  _____)|  ___  || |   | ||  __)   |  __ (  | |   | |(_____  )");
        System.out.println("| (      | (   ) || |   | || (      | (  \\ \\ | |   | |      ) |");
        System.out.println("| )      | )   ( || (___) || (____/\\| )___) )| (___) |/\\____) |");
        System.out.println("|/       |/     \\|(_______)(_______/|/ \\___/ (_______)\\_______)");
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                                   -  This text");
        System.out.println("-splash                                 -  Show splash screen");
        System.out.println("-nosplash                               -  Suppress the splash screen");
        System.out.println("-settings settings.xml                  -  Import settings from file, either exported XML or property file format");
        System.out.println("-export_settings settings.xml           -  Export settings to file");
        System.out.println("-logging logging.properties             -  Load log settings");
        System.out.println("-list                                   -  List available application features");
        System.out.println("-server port                            -  Create instance server on given TCP port");
        System.out.println("-app probe                              -  Launch an application with input arguments");
        System.out.println("-resource  /tmp/example.plt             -  Open an application configuration file with the default application");
        System.out.println("-layout /path/to/Example.memento        -  Start with the specified saved layout instead of the default 'memento'");
        System.out.println("-clean                                  -  Start with a blank workspace. Overrides -app, -resource and -layout.");
        System.out.println("-main org.package.Main                  -  Run alternate application Main");
        System.out.println();
        System.out.println("In 'server' mode, first instance opens UI.");
        System.out.println("Additional calls to open resources are then forwarded to the initial instance.");
        System.out.println();
        System.out.println("The '-resource' parameter can be a URI for a file or web link.");
        System.out.println("The schema 'pv://?PV1&PV2&PV3' is used to pass PV names,");
        System.out.println("and the 'app=..' query parameter picks a specific app for opening the resource.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("-resource '/path/to/file'                                                    - Opens that file with the default application.");
        System.out.println("-resource 'file:/absolute/path/to/file'                                      - Same, but makes the 'file' schema specific.");
        System.out.println("-resource 'http://my.site/path/to/file'                                      - Reads web link, opens with default application.");
        System.out.println("-resource 'file:/abs/path/file?app=display_runtime&MACRO1=value+1&MACRO2=xy' - Opens file with 'display_runtime' app, passing macros.");
        System.out.println("-resource 'pv://?sim://sine&app=probe'                                       - Opens the 'sim://sine' PV with 'probe'.");
        System.out.println("-resource 'pv://?Fred&sim://sine&app=pv_table'                               - Opens two PVs PV with 'pv_table'.");
        System.out.println("-resource '...&target=window'                                                - Opens resource in separate window.");
        System.out.println("-resource '...&target=window@800x600+200+150'                                - Opens resource in separate window sized 800 by 600 at x=200, y=150.");
        System.out.println("-resource '...&target=name_of_pane'                                          - Opens resource in named pane.");
    }
}
