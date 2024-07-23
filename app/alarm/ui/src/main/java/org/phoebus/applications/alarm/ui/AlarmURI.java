/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** Alarm URI helpers
 *
 *  <p>Alarm tools use a URI "alarm://host:port/config_name".
 *  For example, an alarm tree for the "Accelerator" configuration
 *  can be opened with the resource "alarm://localhost/Accelerator?app=alarm_tree"
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmURI
{
    /** URI schema used to refer to an alarm config */
    public static final String SCHEMA = "alarm";

    /** Nme to use for item as query parameter */
    public static final String QUERY_PARAMETER_ITEM_NAME = "itemName";

    /** @param server Kafka server host:port
     *  @param config_name Alarm configuration root
     *  @return URI used to access that alarm configuration, "alarm://host:port/config_name"
     */
    public static URI createURI(final String server, final String config_name) {
        return createURI(server, config_name, null);
    }

    /** @param server Kafka server host:port
     *  @param config_name Alarm configuration root
     *  @param rawQuery raw query for URI
     *  @return URI used to access that alarm configuration, "alarm://host:port/config_name?rawQuery"
     */
    public static URI createURI(final String server, final String config_name, String rawQuery) {
        if (rawQuery != null  && rawQuery.length() > 0)
            return URI.create(SCHEMA + "://" + server + "/" + config_name + "?" + rawQuery);
        else
            return URI.create(SCHEMA + "://" + server + "/" + config_name);
    }

    /** Parse alarm configuration parameters from URI
     *  @param resource "alarm://localhost:9092/Accelerator" or "alarm://localhost:9092/Accelerator?param=value"
     *  @return ["localhost:9092", "Accelerator", null] or ["localhost:9092", "Accelerator", "param=value"]
     *  @throws Exception on error
     */
    public static String[] parseAlarmURI(final URI resource) throws Exception
    {
        if (! SCHEMA.equals(resource.getScheme()))
            throw new Exception("Cannot parse " + resource + ", expecting " + SCHEMA + "://{host}:{port}/{config_name}");

	String[] splitURI = (resource.toString()).split("://");
	// If user provides multiple kafka hosts i.e. host1:port1,host2:port2,host3:port3
	if (splitURI[1].contains(","))
	{
	    String[] splitAuthorityPath = splitURI[1].split("/");
	    return new String[] { splitAuthorityPath[0], splitAuthorityPath[1] };
	}

        // Default to port 9092
        int port = resource.getPort();
        if (port < 0)
            port = 9092;
        String config_name = resource.getPath();
        String rawQuery = resource.getRawQuery();
        if (config_name.startsWith("/"))
            config_name = config_name.substring(1);
        if (config_name.isEmpty())
            throw new Exception("Missing alarm config name in " + resource + ", expecting " + SCHEMA + "://{host}:{port}/{config_name}");
        return new String[] { resource.getHost() + ":" + port, config_name, rawQuery };
    }

    /**
     * Return a map with parameter names and values as key-value pairs for raw query of given resource.
     * @param resource "alarm://localhost:9092/Accelerator" or "alarm://localhost:9092/Accelerator?param=value"
     * @return map with parameter names as keys and parameter values as values for raw query
     */
    public static Map<String, String> getRawQueryParametersValues(URI resource) {
        Map<String, String> map = new HashMap<>();
        String[] queryParametersValues = resource.getRawQuery() != null ? resource.getRawQuery().split("&") : null;
        if (queryParametersValues != null) {
            for (String queryParameterValue : queryParametersValues) {
                String[] parameterValue = queryParameterValue.split("=");
                if (parameterValue != null) {
                    if (parameterValue.length == 2) {
                        map.put(parameterValue[0], parameterValue[1]);
                    } else if (parameterValue.length == 1) {
                        map.put(parameterValue[0], null);
                    }
                }
            }
        }
        return map;
    }

}
