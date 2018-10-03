package org.phoebus.app.viewer3d;

import static org.phoebus.app.viewer3d.Viewer3dPane.logger;

import java.net.URI;
import java.util.logging.Level;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class Viewer3dInstance implements AppInstance
{
    private final AppDescriptor app;
    private final DockItemWithInput tab;

    public Viewer3dInstance(final Viewer3dApp viewerApp, final URI resource)
    {
        app = viewerApp;
        tab = new DockItemWithInput(this, create(resource), resource, null, null);
        
        Platform.runLater(() -> tab.setLabel(app.getDisplayName()));
        
        DockPane.getActiveDockPane().addTab(tab);        
    }
    
    private Node create(URI resource)
    {
        try
        {
            return new Viewer3dPane(resource, this::changeInput);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create 3d Viewer for " + resource, ex);
            return new Label("Cannot create 3d Viewer for " + resource);
        }
    }
    
    private void changeInput(final URI resource)
    {
        tab.setInput(resource);
        Platform.runLater(() -> tab.setLabel(app.getDisplayName()));
    }
    
    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }
}
