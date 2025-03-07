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

package org.phoebus.security.tokens;

import java.util.Arrays;

/**
 * Enum constants for authentication scopes used in dedicated use cases and applications.
 * Restrictions on the name of an {@link AuthenticationScope} value:
 * <ul>
 *     <li>Must match regexp [a-z-]*, i.e. lower case alphanumeric chars, except digits, plus hyphen (-)</li>
 *     <li>Must be unique among other enum names</li>
 * </ul>
 */
public enum AuthenticationScope {

    LOGBOOK("logbook"),
    SAVE_AND_RESTORE("save-and-restore"),
    NEO4J("graph-database"),
    S3("aws-image-bucket"),
    MONGODB("mongodb-ux"),
    MARIADB("mariadb-ux");



    private String name = null;

    private String supportedNamePattern = "[a-z-]*";

    /**
     * Internal use.
     * @param name Valid name
     * @throws IllegalArgumentException if the name does not match [a-z-]*.
     */
    AuthenticationScope(String name) throws IllegalArgumentException{
        if(!name.matches(supportedNamePattern)){
            throw new IllegalArgumentException("Name " + name + " invalid");
        }
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public static AuthenticationScope fromString(String s){
        return Arrays.stream(values()).filter(a -> a.name.equals(s)).findFirst().orElse(null);
    }
}
