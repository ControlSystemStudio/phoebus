package org.phoebus.product;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.phoebus.ui.application.PhoebusApplication;

import javafx.application.Application;

public class Launcher {

    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(Launcher.class.getResourceAsStream("/logging.properties"));

        Logger.getLogger(Launcher.class.getName()).info("Phoebus Launcher");

        // Handle arguments, potentially not even starting the UI
        final List<String> args = new ArrayList<>(Arrays.asList(original_args));
        final Iterator<String> iter = args.iterator();
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

                if (cmd.equals("-settings"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -settings file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    Preferences.importPreferences(new FileInputStream(filename));
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
            }
        }
        catch (Exception ex)
        {
            help();
            System.out.println();
            ex.printStackTrace();
            return;
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
        System.out.println("-help                           -  This text");
        System.out.println("-settings settings.xml          -  Import settings from file");
        System.out.println("-export_settings settings.xml   -  Export settings to file");
    }
}
