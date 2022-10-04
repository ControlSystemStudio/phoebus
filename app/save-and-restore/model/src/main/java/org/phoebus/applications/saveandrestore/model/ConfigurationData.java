/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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

package org.phoebus.applications.saveandrestore.model;

import java.util.List;

/**
 * Class holding data particular to a save-n-restore configuration {@link Node}.
 *
 * Note that certain properties (name, user id, create date) are contained in the {@link Node} object
 * associated with the configuration. The <code>uniqueId</code> is used to create this association, i.e.
 * a {@link ConfigurationData} object as persisted in the remote service will have the same unique id
 * as the associated {@link Node} object.
 */
public class ConfigurationData {

    /**
     * This <b>must</b> be set to the same unique id as the {@link Node} object
     * mapping to the configuration {@link Node}.
     */
    private String uniqueId;
    private List<ConfigPv> pvList;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public List<ConfigPv> getPvList() {
        return pvList;
    }

    public void setPvList(List<ConfigPv> pvList) {
        this.pvList = pvList;
    }

    /**
     * Clones a {@link ConfigurationData} object. The unique id is not copied, as it should not be.
     * @param configurationToClone source {@link Node}
     * @return The cloned {@link Node}
     */
    public static ConfigurationData clone(ConfigurationData configurationToClone){
        ConfigurationData clone = new ConfigurationData();
        clone.setPvList(configurationToClone.getPvList());
        return clone;
    }
}
