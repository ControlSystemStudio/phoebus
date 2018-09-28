package org.phoebus.app.viewer3d;

import java.net.URI;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;

public class Viewer3dApp implements AppResourceDescriptor
{
    public static final String Name = "3d_viewer";
    
    public static final String DisplayName = "3d Viewer";
    
    @Override
    public String getName()
    {
        return Name;
    }

    @Override
    public String getDisplayName()
    {
        return DisplayName;
    }

    @Override
    public AppInstance create()
    {
        return new Viewer3dInstance(this, null);
    }

    @Override
    public AppInstance create(URI resource)
    {
        return new Viewer3dInstance(this, resource);
    }
}
