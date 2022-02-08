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
    public static String jmasar_service_url;
    @Preference
    public static int http_client_readTimeout;
    @Preference
    public static int http_client_connectTimeout;
    @Preference
    public static boolean split_snapshot;
    @Preference
    public static boolean sort_snapshots_time_reversed;
    @Preference
    public static boolean split_saveset;
    @Preference
    public static boolean tree_tableview_enable;
    @Preference
    public static String tree_table_view_hierarchy_parser;
    @Preference
    public static String regex_hierarchy_parser_regex_list;
    @Preference
    public static boolean enableCSVIO;

    static
    {
        AnnotatedPreferences.initialize(Preferences.class, "/save_and_restore_preferences.properties");
    }
}
