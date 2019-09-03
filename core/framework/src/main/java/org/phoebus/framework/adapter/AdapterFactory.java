package org.phoebus.framework.adapter;

import java.util.List;
import java.util.Optional;

/**
 * This is the SPI for contributing an adapter to handle the conversion from one type to another
 *
 * @author kunal
 *
 */
public interface AdapterFactory {

    /**
     * Returns the type of the object that can be processed by this adapter factory
     * @return the adaptable object Class
     */
    public Class getAdaptableObject();

    /**
     * Returns a list of types the adaptable object can be converted too.
     * @return the list of types to which this adapter can convert the supported adaptable type
     */
    public List<? extends Class> getAdapterList();

    /**
     * The function to adapt an object adaptableObject to type adapterType
     * @param adaptableObject
     * @param adapterType
     * @return the adapted object
     */
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType);

}
