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

package org.phoebus.applications.saveandrestore;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

public class Preferences {

    @Preference
    public static boolean sortSnapshotsTimeReversed;
    @Preference
    public static boolean tree_tableview_enable;
    @Preference
    public static boolean enableCSVIO;
    @Preference
    public static int readTimeout;
    @Preference
    public static int search_result_page_size;
    @Preference
    public static String default_search_query;

    static
    {
        AnnotatedPreferences.initialize(Preferences.class, "/save_and_restore_preferences.properties");
    }
}
