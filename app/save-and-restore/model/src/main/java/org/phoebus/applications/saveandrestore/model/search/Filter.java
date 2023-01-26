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

package org.phoebus.applications.saveandrestore.model.search;

import java.util.Date;

/**
 * Simple POJO used to encapsulate a search query and a name associated with an instance of
 * this class. The <code>name</code> field is used as a unique case-sensitive identifier.
 *
 * A username and last modified date is also included.
 */
public class Filter {

    /**
     * Case-sensitive, unique name of a {@link Filter} instance.
     */
    private String name;

    /**
     * A valid query string that defines a {@link Filter}.
     */
    private String queryString;

    /**
     * User identity set when a {@link Filter} is created or updated.
     */
    private String user;

    /**
     * Last updated date set by the service when a {@link Filter} is created or updated.
     */
    private Date lastUpdated;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object other){
        if(!(other instanceof Filter)){
            return false;
        }
        return name.equals(((Filter)other).getName());
    }

    @Override
    public int hashCode(){
        return name.hashCode();
    }
}
