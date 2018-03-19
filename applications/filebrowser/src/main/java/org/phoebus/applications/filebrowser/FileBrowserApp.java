package org.phoebus.applications.filebrowser;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

@SuppressWarnings("nls")
public class FileBrowserApp implements AppDescriptor {

    public static final String Name = "FileBrowser";

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public AppInstance create() {
        return new FileBrowser(this);
    }
}
