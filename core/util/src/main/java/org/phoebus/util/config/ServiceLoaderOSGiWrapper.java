/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for ServiceLoader that works in OSGi as well.
 * <p>
 * OSGi is known to break the ServiceLoader mechanism. Unfortunately, we have
 * not been able to make the standard based solution based on Ares SPI-Fly work.
 * Therefore we have created this wrapper that detects whether the class has
 * been loaded with OSGi. If not, uses the standard ServiceLoader. If so, uses
 * introspection to access the OSGi framework and replicate the functionality.
 * <p>
 * Note that the implementation on OSGi will not give all the nice debugging
 * information that the standard ServiceLoader implementation does. It also is
 * not optimized: makes heavy use of introspection and does not use lazy
 * initialization. It's essentially a hack to make things to work.
 *
 * @author carcassi
 */
public class ServiceLoaderOSGiWrapper {

    public static <T> void load(Class<T> serviceClazz, Logger log, Consumer<T> consumer) {
        log.log(Level.CONFIG, "Fetching {0}s", serviceClazz.getSimpleName());
        int count = 0;
        for (T service : ServiceLoaderOSGiWrapper.load(serviceClazz)) {
            log.log(Level.CONFIG, "Found {0} ({1})", new Object[] {serviceClazz.getSimpleName(), service.getClass().getSimpleName()});
            try {
                consumer.accept(service);
                count++;
            } catch (RuntimeException ex) {
                log.log(Level.WARNING, "Couldn't register " + serviceClazz.getSimpleName() + " (" + service.getClass().getSimpleName() + ")", ex);
            }
        }
        log.log(Level.CONFIG, "Found {0} {1}s", new Object[] {count, serviceClazz.getSimpleName()});
    }

    public static <T> Iterable<T> load(Class<T> serviceClazz) {
        if (isOSGi(serviceClazz)) {
            return loadOSGi(serviceClazz);
        } else {
            return ServiceLoader.load(serviceClazz);
        }
    }

    private static boolean isOSGi(Class<?> serviceClazz) {
        try {
            if (osgi.frameworkUtilClass != null && osgi.getBundle(serviceClazz) != null) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static <T> List<T> loadOSGi(Class<T> serviceClazz) {
        // TODO: improve logging
        Object bundle = osgi.getBundle(serviceClazz);
        System.out.println("Found bundle " + bundle);
        osgi.start(bundle);
        Object bundleContext = osgi.getBundleContext(bundle);
        Object[] bundles = osgi.getBundles(bundleContext);
        System.out.println("Found bundle " + bundleContext);
        System.out.println("Found bundles " + bundles);
        Map<Object, List<String>> bundleToServiceClassnamesMap = new HashMap<>();
        List<T> providers = new ArrayList<>();
        for (Object providerBundle : bundles) {
            URL url = osgi.getEntry(providerBundle, "META-INF/services/" + serviceClazz.getName());
            if (url != null) {
                System.out.println("Found " + url + " in " + providerBundle);
                List<String> classnames = readAllLines(url);
                if (classnames != null) {
                    bundleToServiceClassnamesMap.put(providerBundle, classnames);
                    System.out.println("Providers " + classnames);
                    for (String classname : classnames) {
                        Class<?> providerClass = osgi.loadClass(providerBundle, classname);
                        if (providerClass != null) {
                            try {
                                T provider = serviceClazz.cast(providerClass.newInstance());
                                System.out.println("Provider instance " + provider);
                                providers.add(provider);
                            } catch (InstantiationException | IllegalAccessException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return providers;
    }

    private static OSGiReflection osgi = new OSGiReflection();

    private static class OSGiReflection {

        private final Class<?> frameworkUtilClass = loadClass("org.osgi.framework.FrameworkUtil");
        private final Class<?> bundleClass = loadClass("org.osgi.framework.Bundle");
        private final Class<?> bundleContextClass = loadClass("org.osgi.framework.BundleContext");
        private final Method getBundleMethod = loadMethod(frameworkUtilClass, "getBundle", Class.class);
        private final Method startMethod = loadMethod(bundleClass, "start");
        private final Method getBundleContextMethod = loadMethod(bundleClass, "getBundleContext");
        private final Method getBundlesMethod = loadMethod(bundleContextClass, "getBundles");
        private final Method loadClassMethod = loadMethod(bundleClass, "loadClass", String.class);
        private final Method getEntryMethod = loadMethod(bundleClass, "getEntry", String.class);
        private final boolean osgiEnabled;

        public OSGiReflection() {
            this.osgiEnabled = false;
        }



        private Class<?> loadClass(String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ex) {
                return null;
            }
        }

        private Method loadMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
            if (clazz == null) {
                return null;
            }

            try {
                return clazz.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException | SecurityException ex) {
                Logger.getLogger(ServiceLoaderOSGiWrapper.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        private Object getBundle(Object classFromBundle) {
            try {
                return getBundleMethod.invoke(null, classFromBundle);
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(ServiceLoaderOSGiWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        private void start(Object bundle) {
            try {
                startMethod.invoke(bundle);
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(ServiceLoaderOSGiWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private Object getBundleContext(Object bundle) {
            try {
                return getBundleContextMethod.invoke(bundle);
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(ServiceLoaderOSGiWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        private Object[] getBundles(Object bundleContext) {
            try {
                return (Object[]) getBundlesMethod.invoke(bundleContext);
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(ServiceLoaderOSGiWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        private URL getEntry(Object bundle, String name) {
            try {
                return (URL) getEntryMethod.invoke(bundle, name);
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(ServiceLoaderOSGiWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        private Class<?> loadClass(Object bundle, String name) {
            try {
                return (Class<?>) loadClassMethod.invoke(bundle, name);
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(ServiceLoaderOSGiWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }
    }

    private static List<String> readAllLines(URL url) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"))) {
            List<String> result = new ArrayList<>();
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                result.add(line);
            }
            return result;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
