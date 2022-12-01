/*******************************************************************************
 * Copyright (c) 2017-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import static org.phoebus.pv.PV.logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.pv.RefCountMap.ReferencedEntry;
import org.phoebus.pv.formula.FormulaPVFactory;

/** Pool of {@link PV}s
 *
 *  <p>Allows creating PVs for all known PVFactory services.
 *
 *  <p>Maintains PVs, with a reference count.
 *
 *  <p>A PV is referred to by different names:
 *  <ul>
 *  <li>The name provided by the user: "fred" or "ca://fred",
 *      with or without prefix.
 *      May also contain parameters: "loc://x(3.14)" or "loc://x(14)".
 *  <li>Name used by the type-dependent implementation: "fred"
 *  <li>
 *  </ul>
 *
 *  <p>The PV and the pool use the name provided by the user,
 *  because that way <code>PV.getName()</code> will always return
 *  the expected name.
 *  On the downside, this could create the same underlying PV twice,
 *  with and without the prefix.
 *
 *  <p>Note also that "loc://x(3.14)" and "loc://x(14)" will be treated
 *  as different PVs.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVPool
{
    /** Separator between PV type indicator and rest of PV name.
     *  <p>
     *  This one is URL-like, and works OK with EPICS PVs because
     *  those are unlikely to contain "://" themselves, while
     *  just ":" for example is likely to be inside the PV name
     */
    final public static String SEPARATOR = "://";

    /** Map of PV type prefixes to PV factories */
    final private static Map<String, PVFactory> factories = new HashMap<>();

    /** Default PV name type prefix */
    @Preference(name="default") public static String default_type;

    static
    {
        try
        {
            // Load all PVFactory services
            for (PVFactory factory : ServiceLoader.load(PVFactory.class))
            {
                final String type = factory.getType();
                logger.log(Level.CONFIG, "PV type " + type + ":// provided by " + factory);
                factories.put(type, factory);
            }

            AnnotatedPreferences.initialize(PVPool.class, "/pv_preferences.properties");

            logger.log(Level.INFO, "Default PV type " + default_type + "://");
        }
        catch (Throwable ex)
        {
            logger.log(Level.SEVERE, "Cannot initialize PVPool", ex);
        }
    }

    /** Combination of type and name, <code>type://name</code> */
    public static class TypedName
    {
        /** PV Type */
        public final String type;
        /** Name */
        public final String name;

        /** Analyze PV name
         *  @param type_name PV Name, "name..." or  "type://name..."
         *  @return {@link TypedName}
         */
        public static TypedName analyze(final String type_name)
        {
            final String type, name;

            if (type_name.startsWith("="))
            {   // Special handling of equations, treating "=...." as "eq://...."
                type = FormulaPVFactory.TYPE;
                name = type_name.substring(1);
            }
            else
            {
                final int sep = type_name.indexOf(SEPARATOR);
                if (sep > 0)
                {
                    type = type_name.substring(0, sep);
                    name = type_name.substring(sep+SEPARATOR.length());
                }
                else
                {
                    type = default_type;
                    name = type_name;
                }
            }
            return new TypedName(type, name);
        }

        /** @param type "type"
         *  @param name "name"
         *  @return "type://name"
         */
        public static String format(final String type, final String name)
        {
            return type + SEPARATOR + name;
        }

        private TypedName(final String type, final String name)
        {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return format(type, name);
        }
    }

    /** @param name PV Name, may be "xxx" or "type://xxx"
     *  @param equivalent_pv_prefixes List of equivalent PV prefixes (types), e.g. "ca", "pva"
     *  @return Set of equivalent names, e.g. "xxx", "ca://xxx", "pva://xxx"
     */
    public static Set<String> getNameVariants(final String name, final String [] equivalent_pv_prefixes)
    {
        // First, look for name as given
        final Set<String> variants = new LinkedHashSet<>();
        variants.add(name);
        if (equivalent_pv_prefixes != null  &&  equivalent_pv_prefixes.length > 0)
        {   // Optionally, if the original name is one of the equivalent types ...
            final TypedName typed = TypedName.analyze(name);
            for (String type : equivalent_pv_prefixes)
                if (type.equals(typed.type))
                {
                    // .. add equivalent prefixes, starting with base name
                    variants.add(typed.name);
                    for (String variant : equivalent_pv_prefixes)
                        variants.add(TypedName.format(variant, typed.name));
                    break;
                }
        }
        return variants;
    }

    /** PV Pool
     *  SYNC on 'pool':
     *  Otherwise, two threads concurrently looking for a new PV would both add it.
     */
    final private static RefCountMap<String, PV> pool = new RefCountMap<>();

    /** Singleton */
    private PVPool()
    {
    }

    /** @return Supported PV type prefixes */
    public static Collection<String> getSupportedPrefixes()
    {
        return factories.keySet();
    }

    /** Obtain a PV
     *
     *  <p>Obtains existing PV of that name from pool,
     *  or creates new PV if no existing PV found.
     *
     *  @param name PV name, where prefix might be used to determine the type
     *  @return {@link PV}
     *  @throws Exception on error
     *  @see #releasePV(PV)
     */
    public static PV getPV(final String name) throws Exception
    {
        if (name.isBlank())
            throw new Exception("Empty PV name");
        final TypedName type_name = TypedName.analyze(name);
        final PVFactory factory = factories.get(type_name.type);
        if (factory == null)
            throw new Exception(name + " has unknown PV type '" + type_name.type + "'");

        final String core_name = factory.getCoreName(name);
        final ReferencedEntry<PV> ref = pool.createOrGet(core_name, () -> createPV(factory, name, type_name.name));
        logger.log(Level.CONFIG, () -> "PV '" + ref.getEntry().getName() + "' references: " + ref.getReferences());
        return ref.getEntry();
    }

    private static PV createPV(PVFactory factory, final String name, final String base_name)
    {
        try
        {
            return factory.createPV(name, base_name);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create PV '" + name + "'", ex);
        }
        return null;
    }

    /** @param pv PV to be released */
    public static void releasePV(final PV pv)
    {
        final int references = pool.release(pv.getName());
        if (references <= 0)
        {
            pv.close();
            logger.log(Level.CONFIG, () -> "PV '" + pv.getName() + "' closed");
        }
        else
            logger.log(Level.CONFIG, () -> "PV '" + pv.getName() + "' remaining references: " + references);
    }

    /** @return PVs currently in the pool with reference count information */
    public static Collection<ReferencedEntry<PV>> getPVReferences()
    {
        synchronized (pool)
        {
            return pool.getEntries();
        }
    }
}
