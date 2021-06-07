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

package org.phoebus.olog.es.api.model;

import org.phoebus.logbook.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LogGroupProperty {

    public static final String NAME = "Log Entry Group";
    public static final String ATTRIBUTE_ID = "id";

    public static Property create(){
        Map<String, String> attributes = new HashMap<>();
        attributes.put(ATTRIBUTE_ID, UUID.randomUUID().toString());
        return new OlogProperty(NAME, attributes);
    }
}
