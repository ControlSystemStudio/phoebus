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
 *
 */

package org.phoebus.pv.pvws.models.temp;

import java.util.Map;
import java.util.Set;

public class InfoData {

    private String start_time;
    private String jre;

    private Set<Map.Entry<String, String>> env;

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getJre() {
        return jre;
    }

    public void setJre(String jre) {
        this.jre = jre;
    }

    public Set<Map.Entry<String, String>> getEnv() {
        return env;
    }

    public void setEnv(Set<Map.Entry<String, String>> env) {
        this.env = env;
    }
}
