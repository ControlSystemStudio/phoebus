package org.phoebus.applications.filebrowser;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;

@SuppressWarnings("nls")
public class FileBrowserApp implements AppResourceDescriptor {

    public static final String Name = "file_browser";

    public static final String DisplayName = Messages.DisplayName;

    /** Initial root directory for newly opened file browser */
    @Preference public static File default_root;

    /** Show hidden files (File.isHidden)? */
    @Preference public static boolean show_hidden;

    static
    {
    	AnnotatedPreferences.initialize(FileBrowserApp.class, "/filebrowser_preferences.properties");
    }

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public String getDisplayName()
    {
        return DisplayName;
    }

    @Override
    public AppInstance create() {
        return createWithRoot(default_root);
    }

    @Override
    public AppInstance create(final URI resource)
    {
        // Use ResourceParser.getFile which handles UNC/network paths
        // (URIs with a host/authority component)
        final File file = ResourceParser.getFile(resource);
        if (file != null)
            return createWithRoot(file);
        // Fallback: try using path string
        return createWithRoot(new File(resource.getPath()));
    }


    public AppInstance createWithRoot(final File directory)
    {
        return new FileBrowser(this, directory);
    }

    @Override
    public URL getIconURL()
    {
        return FileBrowserApp.class.getResource("/icons/filebrowser.png");
    }
}
