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

package org.phoebus.logbook.olog.ui;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.phoebus.logbook.LogEntry;

import java.util.ArrayList;
import java.util.List;

public class LogEntryTreeHelper {

    public ObservableList<TreeItem<LogEntry>> createTree(List<LogEntry> logEntries){
        return null;
    }

    private class LogEntryGroup{
        private String id;
        private List<LogEntry> items = new ArrayList<>();

        public LogEntryGroup(String id){
            this.id = id;
        }

        public void addItem(LogEntry entry){
            items.add(entry);
        }

        public List<LogEntry> getItems(){
            return items;
        }

        @Override
        public boolean equals(Object other){
            if(other instanceof LogEntryGroup){
                return ((LogEntryGroup) other).id.equals(id);
            }
            return false;
        }

        @Override
        public int hashCode(){
            return id.hashCode();
        }
    }
}
