/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.csstudio.scan.device.DeviceInfo;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Helper for handling scan_config.xml files
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanConfig
{
    final private static String XML_SCAN_CONFIG = "scan_config";
    final private static String XML_SIMULATION_HOOK = "simulation_hook";
    final private static String XML_PORT = "port";
    final private static String XML_PV = "pv";
    final private static String XML_NAME = "name";
    final private static String XML_NAME_PATTERN = "name_pattern";
    final private static String XML_ALIAS = "alias";
    final private static String XML_SLEW_RATE = "slew_rate";

    private int port = 4810;

    private String simulation_hook = "";

    /** Predefined devices, maybe with alias */
    private final List<DeviceInfo> devices = new ArrayList<>();

    /** Map of PV names to slew rate */
    final private Map<String, Double> pv_slew_rates = new HashMap<String, Double>();

    /** Pattern for PV name and associated slew rate */
    static private class PatternedSlew
    {
        final Pattern pattern;
        final double slew_rate;

        public PatternedSlew(final String pattern, final double slew_rate)
        {
            this.pattern = Pattern.compile(pattern);
            this.slew_rate = slew_rate;
        }

        public boolean matches(final String device_name)
        {
            return pattern.matcher(device_name).matches();
        }
    };

    /** Slew rates for PV name patterns */
    final private List<PatternedSlew> patterned_slew_rates = new ArrayList<PatternedSlew>();

    /** Default slew rate for PVs that were not specified */
    final public static double DEFAULT_SLEW_RATE = 0.05;

    /** Read scan configuration from XML stream
     *  @param stream Stream for XML content
     *  @throws Exception on error
     */
    public ScanConfig(final InputStream stream) throws Exception
    {
        read(stream);
    }

    /** @return REST port */
    public int getPort()
    {
        return port;
    }

    public List<String> getScriptPaths()
    {
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    public List<String> getPreScanPaths()
    {
        return Collections.emptyList();
    }

    public List<String> getPostScanPaths()
    {
        return Collections.emptyList();
    }

    /** @return Jython class to use for simulation hook. May be empty */
    public String getSimulationHook()
    {
        return simulation_hook;
    }

    public String getMacros()
    {
        // TODO read macros from config file
        return "";
    }

    public double getValueCheckTimeout()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getStatusPvPrefix()
    {
        // TODO Status PV Prefix
        return null;
    }

    /** @return {@link DeviceInfo}s read from file */
    public List<DeviceInfo> getDevices()
    {
        return devices;
    }

    /** Get slew rate for device, otherwise returning default */
    public double getSlewRate(final String device_name)
    {
        Double slew = pv_slew_rates.get(device_name);
        if (slew != null)
            return slew;

        // Check pattern
        for (PatternedSlew ps : patterned_slew_rates)
            if (ps.matches(device_name))
                return ps.slew_rate;
        return DEFAULT_SLEW_RATE;
    }

    /** Read device configuration from XML stream
     *  @param stream Stream for XML content
     *  @return {@link DeviceInfo}s read from stream
     *  @throws Exception on error
     */
    private void read(final InputStream stream) throws Exception
    {
        final Element xml = XMLUtil.openXMLDocument(stream, XML_SCAN_CONFIG);

        XMLUtil.getChildInteger(xml, XML_PORT)
               .ifPresent(p -> port = p);

        XMLUtil.getChildString(xml, XML_SIMULATION_HOOK)
               .ifPresent(hook -> simulation_hook = hook);

        for (Element pv : XMLUtil.getChildElements(xml, XML_PV))
        {
            final Optional<Double> slew_rate = XMLUtil.getChildDouble(pv, XML_SLEW_RATE);
            final Optional<String> name = XMLUtil.getChildString(pv, XML_NAME);
            if (name.isPresent())
            {   // Got name, maybe with alias and slew rate
                final String alias = XMLUtil.getChildString(pv, XML_ALIAS).orElse(name.get());
                devices.add(new DeviceInfo(name.get(), alias));
                if (slew_rate.isPresent())
                    pv_slew_rates.put(name.get(), slew_rate.get());
            }
            else
            {   // Check if it's a pattern, which then requires a slew rate
                final String pattern = XMLUtil.getChildString(pv, XML_NAME_PATTERN)
                                              .orElseThrow(() -> new Exception("Missing <pv> <name> or <name_pattern>"));
                if (! slew_rate.isPresent())
                    throw new Exception("Missing <slew_rate> for <pv> <name_pattern>");
                patterned_slew_rates.add(new PatternedSlew(pattern, slew_rate.get()));
            }
        }
    }

    public double getOldScanRemovalMemoryThreshold()
    {
        // TODO Auto-generated method stub
        return 50.0;
    }

    public double getLogCommandReadTimeout()
    {
        // TODO Auto-generated method stub
        return 30;
    }
}
