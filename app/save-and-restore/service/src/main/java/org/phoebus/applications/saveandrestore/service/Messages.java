/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.service;

import org.phoebus.framework.nls.NLS;

public class Messages {

    public static String compositeSnapshotConsistencyCheckFailed;
    public static String copyOrMoveNotAllowedBody;
    public static String createNodeFailed;
    public static String createCompositeSnapshotFailed;
    public static String createConfigurationFailed;
    public static String deleteFilterFailed;
    public static String updateConfigurationFailed;
    public static String updateCompositeSnapshotFailed;
    public static String saveFilterFailed;
    public static String searchFailed;
    public static String saveSnapshotFailed;
    public static String updateNodeFailed;

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation
    }

}
