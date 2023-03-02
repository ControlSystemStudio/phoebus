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
 *
 */

package org.phoebus.applications.saveandrestore.model;

import java.util.List;

/**
 * Class wrapping a {@link Tag} and a list of unique {@link Node} ids.
 * Used to manage a {@link Tag} (add or delete) on a list of target {@link Node}s.
 */
public class TagData {

    private Tag tag;
    private List<String> uniqueNodeIds;

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public List<String> getUniqueNodeIds() {
        return uniqueNodeIds;
    }

    public void setUniqueNodeIds(List<String> uniqueNodeIds) {
        this.uniqueNodeIds = uniqueNodeIds;
    }
}
