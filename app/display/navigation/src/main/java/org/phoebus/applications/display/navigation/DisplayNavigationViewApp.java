package org.phoebus.applications.display.navigation;

import org.csstudio.display.builder.model.DisplayModel;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

public class DisplayNavigationViewApp implements AppResourceDescriptor {

    static final Logger logger = Logger.getLogger(DisplayNavigationViewApp.class.getName());
    public static final String NAME = "display_nav_view";
    public static final String DISPLAYNAME = "Display Navigation View";

    @Override
    public void start() {
        logger.info("Loading " + DISPLAYNAME);
    }
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAYNAME;
    }


    @Override
    public List<String> supportedFileExtentions() {
        return DisplayModel.FILE_EXTENSIONS;
    }

    @Override
    public AppInstance create() {
        return new DisplayNavigationView(this);
    }

    @Override
    public AppInstance create(URI resource) {
         DisplayNavigationView displayNavigationView = new DisplayNavigationView(this);
         // Set the file;
         return displayNavigationView;
    }

}
