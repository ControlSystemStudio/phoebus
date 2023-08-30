/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.epics.pva.data.nt;

import org.epics.pva.data.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Normative URI type
 * <p>
 * NTURI is the EPICS V4 Normative Type that describes a Uniform Resource Identifier (URI) bib:uri.
 * </p>
 * NTURI  :=
 * <ul>
 * structure
 * <li>
 * <ul>
 * <li>string scheme</li>
 * <li>string authority   : opt</li>
 * <li>string path</li>
 * <li>structure query    : opt
 * <ul>
 *     <li>{string | double | int <field-name>}0+
 *  *     {<field-type> <field-name>}0+</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 *     Zero or more pvData Fields whose type are not defined until runtime, may be added to an NTURI
 *     by an agent creating an NTURI. This is the mechanism by which complex data may be sent to a
 *     channel. For instance a table of magnet setpoints.
 * </p>
 */
public class PVAURI extends PVAStructure {
    public static final String STRUCT_NAME = "epics:nt/NTURI:1.0";

    private static final String SCHEME_NAME = "scheme";
    private static final String AUTHORITY_NAME = "authority";
    private static final String PATH_NAME = "path";
    private static final String QUERY_NAME = "query";
    private static final String DEFAULT_SCHEME_NAME = "pva";

    private final PVAString scheme;
    private final PVAString authority;
    private final PVAString path;
    private final PVAStructure query;


    private PVAURI(String name, PVAString scheme, PVAString authority, PVAString path, PVAStructure query) {
        super(name, STRUCT_NAME, Arrays.stream(new PVAData[] {scheme, path, authority, query}).filter(Objects::nonNull).toArray(PVAData[]::new));
        this.scheme = scheme;
        this.authority = authority;
        this.path = path;
        this.query = query;
    }

    private PVAURI(String name, PVAString scheme, PVAString path) {
        this(name, scheme, new PVAString(AUTHORITY_NAME), path, new PVAStructure(QUERY_NAME, "structure"));
    }

    /**
     * Set all non-optional parameters. Uses default scheme of "pva"
     *
     * @param name   Name of PV
     * @param path   The path gives the channel from which data is being requested.
     */
    public PVAURI(String name, String path) {
        this(name, DEFAULT_SCHEME_NAME, path);
    }

    /**
     * Set all non-optional parameters.
     *
     * @param name   Name of PV
     * @param scheme The scheme name must be given. For the pva scheme, the scheme name is “pva”. The pva scheme
     *               implies but does not require use of the pvAccess protocol.
     * @param path   The path gives the channel from which data is being requested.
     */
    public PVAURI(String name, String scheme, String path) {
        this(name, new PVAString(SCHEME_NAME, scheme), new PVAString(PATH_NAME, path));
    }

    /**
     * Set all the parameters of the NTURI
     *
     * @param name      Name of PV
     * @param scheme    The scheme name must be given. For the pva scheme, the scheme name is “pva”. The pva scheme
     *                  implies but does not require use of the pvAccess protocol.
     * @param authority If given, then the IP name or address of an EPICS network pvAccess or channel access server.
     * @param path      The path gives the channel from which data is being requested.
     * @param query     A name value system for passing parameters. The types of the argument value MUST be drawn from
     *                  the following restricted set of scalar types: double, int, or string.
     */
    public PVAURI(String name, String scheme, String authority, String path, Map<String, String> query) {
        this(name, new PVAString(SCHEME_NAME, scheme), new PVAString(AUTHORITY_NAME, authority),
                new PVAString(PATH_NAME, path), queryFromMap(query));
    }

    private static PVAStructure queryFromMap(Map<String, String> query) {
        if (query == null) {
            return new PVAStructure(QUERY_NAME, "structure");
        }
        return new PVAStructure(QUERY_NAME, "structure", query.entrySet().stream().map(e -> new PVAString(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    public String getScheme() {
        return scheme.get();
    }

    public String getAuthority() {
        return authority.get();
    }

    public String getPath() {
        return path.get();
    }

    /**
     * Gets the query in a map format
     *
     * @return Returns the query in a Map<String, String>, returns empty
     *         an empty map if the query is null.
     * @throws NotValueException If a query in the queries structure
     *                           does not implement {@link PVAValue}
     */
    public Map<String, String> getQuery() throws NotValueException {
        Map<String, String> queries = new HashMap<>();
        if (this.query == null) {
            return queries;
        }
        for (PVAData q: this.query.get()) {
            if (q instanceof PVAValue) {
                PVAValue queryString = (PVAString) q;
                queries.put(q.getName(), queryString.formatValue());
            } else {
                throw new NotValueException("query input " + q + " does not implement PVAValue");
            }
        }
        return queries;
    }

    /**
     * Converts from a generic PVAStructure to PVAURI
     *
     * @param structure Input structure
     * @return Representative URI
     */
    public static PVAURI fromStructure(PVAStructure structure) {
        if (structure != null && structure.getStructureName().equals(STRUCT_NAME)) {
            final PVAString scheme = structure.get(SCHEME_NAME);
            final PVAString path = structure.get(PATH_NAME);
            final PVAString authority = structure.get(AUTHORITY_NAME);
            final PVAStructure query = structure.get(QUERY_NAME);
            return new PVAURI(structure.getName(), scheme, authority, path, query);
        }
        return null;
    }
}
