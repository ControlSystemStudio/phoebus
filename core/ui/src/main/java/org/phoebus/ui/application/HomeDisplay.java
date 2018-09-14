package org.phoebus.ui.application;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.net.URI;
import java.util.logging.Level;

public class HomeDisplay
{
    private final URI resource;
    private final String description;
    
    public static HomeDisplay parse(final String specification)
    {
        URI resource = null;
        String description = null;
        
        if (! specification.trim().isEmpty())
        {
            try
            {
                final int sep = specification.lastIndexOf(',');
                if (sep > 0)
                {
                    resource = new URI(specification.substring(0, sep).trim());
                    description = specification.substring(sep+1).trim();
                }
                else
                {
                    resource = new URI(specification.trim());
                    description = resource.getPath();
                }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot parse home resource '" + specification + "'", ex);
            }
        }   
        
        return new HomeDisplay(resource, description);
    }
    
    public HomeDisplay(final URI resource, final String description)
    {
        this.resource = resource;
        this.description = description;
    }
    
    public URI getResource()
    {
        return resource;
    }
    
    public String getDescription()
    {
        return description;
    }
}
