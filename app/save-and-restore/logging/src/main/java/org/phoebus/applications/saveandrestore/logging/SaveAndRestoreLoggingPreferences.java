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

package org.phoebus.applications.saveandrestore.logging;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;

import java.util.ArrayList;
import java.util.List;

public class SaveAndRestoreLoggingPreferences {

    public static List<String> logbooks = new ArrayList<>();
    public static List<String> tags = new ArrayList<>();

    @Preference
    public static String level;

    static {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(SaveAndRestoreLoggingPreferences.class, "/save_and_restore_logging.properties");

        String logbooksString = prefs.get("logbooks");
        if(logbooksString != null && !logbooksString.isEmpty()){
            String[] logbooksList = logbooksString.split(",");
            for(String logbook : logbooksList){
                logbooks.add(logbook.trim());
            }
        }

        String tagsString = prefs.get("tags");
        if(tagsString != null && !tagsString.isEmpty()){
            String[] tagsList = tagsString.split(",");
            for(String tag : tagsList){
                tags.add(tag.trim());
            }
        }
    }
}
