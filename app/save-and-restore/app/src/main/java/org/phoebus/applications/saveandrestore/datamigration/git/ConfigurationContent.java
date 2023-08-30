/*
 * *
 *  * Copyright (C) 2019 European Spallation Source ERIC.
 *  * <p>
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  * <p>
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * <p>
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.phoebus.applications.saveandrestore.datamigration.git;

import java.util.Collections;
import java.util.List;


/**
 *
 * <code>SaveSetContent</code> provides the contents of a configuration file. This class serves only as a container of the
 * loaded data and does not provide any other functionality.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public final class ConfigurationContent {

    private final List<ConfigurationEntry> entries;
    private final String description;

    /**
     * Constructs a new configuration content.
     *
     * @param description the description of the file
     * @param entries the list of PV entries
     */
    ConfigurationContent(String description, List<ConfigurationEntry> entries) {
        this.entries = Collections.unmodifiableList(entries);
        this.description = description;
    }

    /**
     * Return the entries of this configuration. Each entry contain the information about a single PV entry in the configuration
     * file definition.
     *
     * @return the entries
     */
    public List<ConfigurationEntry> getEntries() {
        return entries;
    }

    /**
     * Return the description of the configuration.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
