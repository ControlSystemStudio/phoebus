package org.phoebus.logbook.olog.ui.write;

import org.phoebus.logbook.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 *
 */
public class LogPropertyFactory {

    private ServiceLoader<LogPropertyProvider> loader;
    private static LogPropertyFactory logPropertyService;
    private static List<LogPropertyProvider> factories = new ArrayList<LogPropertyProvider>();

    private LogPropertyFactory() {
        // Load available adapter factories
        loader = ServiceLoader.load(LogPropertyProvider.class);
        loader.stream().forEach(p -> {
            factories.add(p.get());
        });
    }

    /**
     * Returns the instance logbook service instance
     *
     * @return
     */
    public static LogPropertyFactory getInstance() {
        if (logPropertyService == null) {
            logPropertyService = new LogPropertyFactory();
        }
        return logPropertyService;
    }

    public List<Property> getLogProperties() {
        return factories.stream()
                        .map(LogPropertyProvider::getProperty)
                        .collect(Collectors.toList());
    }
}
