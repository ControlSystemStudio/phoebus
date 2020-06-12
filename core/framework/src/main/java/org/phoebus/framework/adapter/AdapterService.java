package org.phoebus.framework.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A service which manages the conversion of different types of selection from one type to another.
 *
 * @author Kunal Shroff
 *
 */
@SuppressWarnings("rawtypes")
public class AdapterService {

    static final java.lang.String SERVICE_NAME = "AdapterService";

    private static ServiceLoader<AdapterFactory> loader;
    private static Logger logger = Logger.getLogger(AdapterService.class.getName());

    /**
     * A map where the key is a type mapped to a list of adapter that can convert to this type.
     */
    private static Map<Class, List<AdapterFactory>> adapters = new HashMap<Class, List<AdapterFactory>>();
    /**
     * A map where the key is a type mapped to a list of adapters that can process this type to other types.
     */
    private static Map<Class, List<AdapterFactory>> adaptables = new HashMap<Class, List<AdapterFactory>>();

    static {
        loader = ServiceLoader.load(AdapterFactory.class);
        loader.stream().forEach(p -> {
            AdapterFactory adapterFactory = p.get();
            adapterFactory.getAdapterList().forEach(adaptableType -> {
                if (!adapters.containsKey(adaptableType)) {
                    adapters.put(adaptableType, new ArrayList<>());
                }
                adapters.get(adaptableType).add(adapterFactory);
            });
            Class adaptable = adapterFactory.getAdaptableObject();
            if (!adaptables.containsKey(adaptable)) {
                adaptables.put(adaptable, new ArrayList<>());
            }
            adaptables.get(adaptable).add(adapterFactory);
        });
    }

    private AdapterService()
    {
    }

    /**
     * Returns a list of all the {@link AdapterFactory}s that can adapt TO the type cls
     * @param cls the type for which we want AdapterFactories to be adapt to
     * @return List of {@link AdapterFactory}s that can adapt to type cls
     */
    public List<AdapterFactory> getAdapters(Class cls)
    {
        if(adapters.get(cls) != null)
        {
            return adapters.get(cls);
        } else
        {
            return Collections.emptyList();
        }
    }

    /**
     * Returns a list of {@link AdapterFactory}s which can adapt this type to other types
     * @param cls the class for which adapter factories are wanted.
     * @return List of {@link AdapterFactory}s that can handle cls
     */
    public static List<AdapterFactory> getAdaptersforAdaptable(Class cls)
    {
        if(adaptables.get(cls) != null)
        {
            return adaptables.get(cls);
        } else
        {
            return Collections.emptyList();
        }
    }

    /**
     * Adapts the adaptableObject to type adapterType using registered adaptor factories.
     *
     * @param adaptableObject
     * @param adapterType
     * @return an {@link Optional} with the the adapted object or empty
     */
    public static <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType)
    {
        if(adapterType.isInstance(adaptableObject))
        {
            return Optional.of(adapterType.cast(adaptableObject));
        }
        List<AdapterFactory> factories = AdapterService.getAdaptersforAdaptable(adaptableObject.getClass()).stream().filter(factory -> {
            return factory.getAdapterList().contains(adapterType);
        }).collect(Collectors.toList());
        if(factories.isEmpty())
        {
            logger.warning("failed to adapt the object " + adaptableObject + " due to no avaiable adaptor factories.");
        } else if(factories.size() == 1)
        {
            return factories.get(0).adapt(adaptableObject, adapterType);
        } else if(factories.size() >= 1)
        {
            logger.warning("failed to adapt the object " + adaptableObject + " due to multiple factories " + factories.size());
        }
        return Optional.empty();
    }
}
