package org.phoebus.framework.adapter;

import java.util.List;
import java.util.Optional;

public interface AdapterFactory {

    /**
     * Returns the type of the object that can be processed by this adapter factory
     * @return the adaptable object Class
     */
    public Class getAdaptableObject();

    /**
     * 
     * @return the list of types to which this adapter can convert the supported adaptable type
     */
    public List<? extends Class> getAdapterList();

    /**
     * 
     * @param adaptableObject
     * @param adapterType
     * @return
     */
    public Optional<?> getAdapter(Object adaptableObject, Class adapterType);

}
