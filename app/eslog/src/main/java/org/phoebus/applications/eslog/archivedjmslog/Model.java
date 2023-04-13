package org.phoebus.applications.eslog.archivedjmslog;

public class Model
{
    protected PropertyFilter[] filters = null;

    public void setFilters(PropertyFilter[] filters)
    {
        synchronized (this)
        {
            this.filters = filters;
        }
    }
}
