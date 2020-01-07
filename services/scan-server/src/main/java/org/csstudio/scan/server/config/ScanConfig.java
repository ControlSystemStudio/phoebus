/*******************************************************************************
 * Copyright (c) 2011-2020 Oak Ridge National Laboratory.
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.csstudio.scan.device.DeviceInfo;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.util.time.TimeDuration;
import org.w3c.dom.Element;

/** Helper for handling scan_config.xml files
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanConfig
{
    final private static String XML_ALIAS = "alias",
                                XML_DATALOG = "data_log",
                                XML_MACROS = "macros",
                                XML_MAXIMUM = "maximum",
                                XML_MINIMUM = "minimum",
                                XML_NAME = "name",
                                XML_NAME_PATTERN = "name_pattern",
                                XML_OLD_SCAN_REMOVAL_THRESHOLD = "old_scan_removal_threadhold",
                                XML_PATH = "path",
                                XML_PORT = "port",
                                XML_POST_SCAN = "post_scan",
                                XML_PRE_SCAN = "pre_scan",
                                XML_PV = "pv",
                                XML_READ_TIMEOUT = "read_timeout",
                                XML_SCAN_CONFIG = "scan_config",
                                XML_SIMULATION_HOOK = "simulation_hook",
                                XML_SLEW_RATE = "slew_rate",
                                XML_STATUS_PV_PREFIX = "status_pv_prefix";

    private int port = 4810;

    private String data_log = "/tmp/scan_log_db";

    private final List<String> pre_scan = new ArrayList<>();
    private final List<String> post_scan = new ArrayList<>();

    private String status_pv_prefix = "";

    private static Duration read_timeout = TimeDuration.ofSeconds(20);

    private final List<String> script_paths = new ArrayList<>();

    private String simulation_hook = "";

    private String macros = "";

    private double old_scan_removal_threadhold = 50.0;

    /** Predefined devices, maybe with alias */
    private final List<DeviceInfo> devices = new ArrayList<>();

    /** Map from alias to device name */
    private final Map<String, String> aliases = new HashMap<>();

    /** Map of PV names to slew rate */
    private final Map<String, Double> pv_slew_rates = new HashMap<>();

    /** Map of PV names to PV for its minimum value */
    private final Map<String, String> pv_mimimum = new HashMap<>();

    /** Map of PV names to PV for its maximum value */
    private final Map<String, String> pv_maximum = new HashMap<>();

    /** Pattern for PV name and associated slew rate */
    private static class PatternedSlew
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
    private final List<PatternedSlew> patterned_slew_rates = new ArrayList<>();


    /** Default slew rate for PVs that were not specified */
    public static final double DEFAULT_SLEW_RATE = 0.05;

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

    public String getDataLogParm()
    {
        return data_log;
    }

    public List<String> getPreScanPaths()
    {
        return pre_scan;
    }

    public List<String> getPostScanPaths()
    {
        return post_scan;
    }

    public String getStatusPvPrefix()
    {
        return status_pv_prefix;
    }

    public Duration getReadTimeout()
    {
        return read_timeout;
    }

    public List<String> getScriptPaths()
    {
        return script_paths;
    }

    /** @return Jython class to use for simulation hook. May be empty */
    public String getSimulationHook()
    {
        return simulation_hook;
    }

    public String getMacros()
    {
        return macros;
    }

    public double getOldScanRemovalMemoryThreshold()
    {
        return old_scan_removal_threadhold;
    }

    /** @return {@link DeviceInfo}s read from config file */
    public List<DeviceInfo> getDevices()
    {
        return devices;
    }

    private String resolveAlias(final String device_name)
    {
        return aliases.getOrDefault(device_name, device_name);
    }

    /** Get slew rate for device, otherwise returning default */
    public double getSlewRate(final String device_name)
    {
        final String actual = resolveAlias(device_name);
        Double slew = pv_slew_rates.get(actual);
        if (slew != null)
            return slew;

        // Check pattern
        for (PatternedSlew ps : patterned_slew_rates)
            if (ps.matches(device_name))
                return ps.slew_rate;
        return DEFAULT_SLEW_RATE;
    }

    /** @param device_name
     *  @return PV for range minimum, or <code>null</code>
     */
    public String getMinimumPV(final String device_name)
    {
        final String actual = resolveAlias(device_name);
        return pv_mimimum.get(actual);
    }

    /** @param device_name
     *  @return PV for range maximum, or <code>null</code>
     */
    public String getMaximumPV(final String device_name)
    {
        final String actual = resolveAlias(device_name);
        return pv_maximum.get(actual);
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

        XMLUtil.getChildString(xml, XML_DATALOG)
               .ifPresent(parm -> data_log = parm);

        for (Element path : XMLUtil.getChildElements(xml, XML_PRE_SCAN))
            pre_scan.add(XMLUtil.getString(path));

        for (Element path : XMLUtil.getChildElements(xml, XML_POST_SCAN))
            post_scan.add(XMLUtil.getString(path));

        XMLUtil.getChildString(xml, XML_STATUS_PV_PREFIX)
               .ifPresent(prefix -> status_pv_prefix = prefix);

        XMLUtil.getChildInteger(xml, XML_READ_TIMEOUT)
               .ifPresent(sec -> read_timeout = TimeDuration.ofSeconds(sec));

        for (Element path : XMLUtil.getChildElements(xml, XML_PATH))
            script_paths.add(XMLUtil.getString(path));

        XMLUtil.getChildString(xml, XML_SIMULATION_HOOK)
               .ifPresent(hook -> simulation_hook = hook);

        XMLUtil.getChildString(xml, XML_MACROS)
               .ifPresent(m -> macros = m);

        XMLUtil.getChildDouble(xml, XML_OLD_SCAN_REMOVAL_THRESHOLD)
               .ifPresent(perc -> old_scan_removal_threadhold = perc);

        for (Element pv : XMLUtil.getChildElements(xml, XML_PV))
        {
            final Optional<Double> slew_rate = XMLUtil.getChildDouble(pv, XML_SLEW_RATE);
            final Optional<String> name = XMLUtil.getChildString(pv, XML_NAME);
            if (name.isPresent())
            {   // Got name, maybe with alias, slew rate, limits
                final Optional<String> alias = XMLUtil.getChildString(pv, XML_ALIAS);
                if (alias.isPresent())
                {
                    devices.add(new DeviceInfo(name.get(), alias.get()));
                    aliases.put(alias.get(), name.get());
                }
                else
                    devices.add(new DeviceInfo(name.get(), name.get()));

                slew_rate.ifPresent(rate ->  pv_slew_rates.put(name.get(), rate));
                XMLUtil.getChildString(pv, XML_MINIMUM)
                       .ifPresent(limit -> pv_mimimum.put(name.get(), limit));
                XMLUtil.getChildString(pv, XML_MAXIMUM)
                       .ifPresent(limit -> pv_maximum.put(name.get(), limit));
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

}
