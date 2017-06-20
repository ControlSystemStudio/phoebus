package org.phoebus.framework.adapter;

import java.util.List;

public interface AdapterFactory {

    public Object getAdapter(Object adaptableObject, Class adapterType);
    
    public List<Class> getAdapterList();
}
