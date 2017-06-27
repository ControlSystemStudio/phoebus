package org.phoebus.framework.adapter;

import java.util.List;
import java.util.Optional;

public interface AdapterFactory {

    /**
     * 
     * @return
     */
    public Class getAdaptableObject();

    /**
     * 
     * @return
     */
    public List<? extends Class> getAdapterList();

    /**
     * 
     * @param adaptableObject
     * @param adapterType
     * @return
     */
    public Optional getAdapter(Object adaptableObject, Class adapterType);

}
