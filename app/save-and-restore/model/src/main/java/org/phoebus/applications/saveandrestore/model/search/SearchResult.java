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

package org.phoebus.applications.saveandrestore.model.search;

import org.phoebus.applications.saveandrestore.model.Node;

import java.util.List;

public class SearchResult {

    /**
     * The total number of hits matching a search query. Note that this need not be
     * the same as the size of {@link #nodes}, e.g. in a pagination search where search can
     * specify "from" and "size".
     */
    private int hitCount;

    /**
     * The list of {@link Node}s matching a search query, taking into account potential "pagination"
     * parameters (from + size).
     */
    private List<Node> nodes;

    public SearchResult(){

    }

    public SearchResult(int hitCount, List<Node> nodes){
        this.hitCount = hitCount;
        this.nodes = nodes;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
}
