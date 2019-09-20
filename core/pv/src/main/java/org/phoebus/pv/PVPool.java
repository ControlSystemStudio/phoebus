/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import static org.phoebus.pv.PV.logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;

import org.phoebus.framework.preferences.PreferencesReader;
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
    public static final String DEFAULT = "default";

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
    private static String default_type = "ca";

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

            final PreferencesReader prefs = new PreferencesReader(PVPool.class, "/pv_preferences.properties");
            default_type = prefs.get(DEFAULT);

            logger.log(Level.INFO, "Default PV type " + default_type + "://");
        }
        catch (Throwable ex)
        {
            logger.log(Level.SEVERE, "Cannot initialize PVPool", ex);
        }
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
        final String[] prefix_base = analyzeName(name);
        final PVFactory factory = factories.get(prefix_base[0]);
        if (factory == null)
            throw new Exception(name + " has unknown PV type '" + prefix_base[0] + "'");

        final String core_name = factory.getCoreName(name);
        final ReferencedEntry<PV> ref = pool.createOrGet(core_name, () -> createPV(factory, name, prefix_base[1]));
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

    /** Analyze PV name
     *  @param name PV Name, "base..." or  "prefix://base..."
     *  @return Array with type (or default) and base name
     */
    private static String[] analyzeName(final String name)
    {
        final String type, base;

        if (name.startsWith("="))
        {   // Special handling of equations, treating "=...." as "eq://...."
            type = FormulaPVFactory.TYPE;
            base = name.substring(1);
        }
        else
        {
            final int sep = name.indexOf(SEPARATOR);
            if (sep > 0)
            {
                type = name.substring(0, sep);
                base = name.substring(sep+SEPARATOR.length());
            }
            else
            {
                type = default_type;
                base = name;
            }
        }
        return new String[] { type, base };
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
