package org.phoebus.framework.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class AdapterService {

    static final java.lang.String SERVICE_NAME = "AdapterService";

    private static AdapterService adapterService;
    private ServiceLoader<AdapterFactory> loader;

    @SuppressWarnings("rawtypes")
    private Map<Class, List<AdapterFactory>> adapters = new HashMap<Class, List<AdapterFactory>>();

    @SuppressWarnings("rawtypes")
private Map<String, List<AdapterFactory>> adaptables = new HashMap<String, List<AdapterFactory>>();

    private AdapterService() {
        // Load available adapter factories
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
            System.out.println(adaptable.getName() + " " + adaptable.toString());
            if(!adaptables.containsKey(adaptable.getName())) {
                adaptables.put(adaptable.getName(), new ArrayList<>());
            }
            adaptables.get(adaptable.getName()).add(adapterFactory);
        });
    }

    public static AdapterService getInstance() {
        if (adapterService == null) {
            adapterService = new AdapterService();
        }
        return adapterService;
    }

    /**
     * 
     * @param cls
     * @return
     */
    public Optional<List<AdapterFactory>> getAdapters(Class cls) {
        return Optional.ofNullable(adapters.get(cls.getName()));
    }

    /**
     * 
     * @param cls
     * @return
     */
    public Optional<List<AdapterFactory>> getAdaptersforAdaptable(Class cls) {
        return Optional.ofNullable(adaptables.get(cls.getName()));
    }
}
