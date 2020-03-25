package org.phoebus.applications.display.navigation;

import javafx.scene.image.Image;
import org.csstudio.display.builder.model.DisplayModel;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.javafx.ImageCache;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

public class DisplayNavigationViewApp implements AppResourceDescriptor {

    static final Logger logger = Logger.getLogger(DisplayNavigationViewApp.class.getName());
    public static final String NAME = "display_nav_view";
    public static final String DISPLAYNAME = "Display Navigation View";

    static final Image icon = ImageCache.getImage(DisplayNavigationViewApp.class, "/icons/navigation-tree-16.png");

    @Override
    public void start()
    {
        logger.info("Loading " + DISPLAYNAME);
    }
    
    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAYNAME;
    }

    @Override
    public URL getIconURL()
    {
        return DisplayNavigationViewApp.class.getResource("/icons/navigation-tree-16.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return DisplayModel.FILE_EXTENSIONS;
    }

    @Override
    public AppInstance create()
    {
        return new DisplayNavigationView(this);
    }

    @Override
    public AppInstance create(URI resource)
    {
         DisplayNavigationView displayNavigationView = new DisplayNavigationView(this);
         // Set File
         displayNavigationView.setResource(resource);
         return displayNavigationView;
    }

}
