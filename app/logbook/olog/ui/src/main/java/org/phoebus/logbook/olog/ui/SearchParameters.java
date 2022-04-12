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

import com.google.common.base.Strings;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Class encapsulating the various logbook search parameters as set and manipulated in the log entry
 * viewers. It includes behaviors to save the UI controllers from maintaining change listeners, while
 * offering an API to let UI controls bind in both directions.
 */
public class SearchParameters implements ObservableValue<String> {

    private SimpleStringProperty title = new SimpleStringProperty();
    private SimpleStringProperty text = new SimpleStringProperty();
    private SimpleStringProperty author = new SimpleStringProperty();
    private SimpleStringProperty level = new SimpleStringProperty();
    private SimpleStringProperty tags = new SimpleStringProperty();
    private SimpleStringProperty logbooks = new SimpleStringProperty();
    private SimpleStringProperty startTime = new SimpleStringProperty();
    private SimpleStringProperty endTime = new SimpleStringProperty();
    private SimpleStringProperty attachments = new SimpleStringProperty();


    /**
     * For internal purposes: when any of the string properties changes, the listeners (UI controllers)
     * will be notified and the new "value" will be computed based on this map, which in turn is
     * updated whenever any of the properties changes.
     */
    private Map<Keys, String> searchParameters = new HashMap<>();

    private final List<InvalidationListener> invalidationListeners = new ArrayList<>();
    private final List<ChangeListener<? super String>> changeListeners = new ArrayList<>();

    public SearchParameters() {
        title.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.TITLE, newValue);
            notifyListeners();
        });
        text.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.DESC, newValue);
            notifyListeners();
        });
        author.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.OWNER, newValue);
            notifyListeners();
        });
        level.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.LEVEL, newValue);
            notifyListeners();
        });
        tags.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.TAGS, newValue);
            notifyListeners();
        });
        logbooks.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.LOGBOOKS, newValue);
            notifyListeners();
        });
        startTime.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.STARTTIME, newValue);
            notifyListeners();
        });
        endTime.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.ENDTIME, newValue);
            notifyListeners();
        });
        attachments.addListener((observable, oldValue, newValue) -> {
            updateMap(Keys.ATTACHMENTS, newValue);
            notifyListeners();
        });
    }

    private void updateMap(Keys key, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            searchParameters.remove(key);
        } else {
            searchParameters.put(key, newValue);
        }
        notifyListeners();
    }

    public void addListener(InvalidationListener listener) {
        invalidationListeners.add(listener);
    }

    public void removeListener(InvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    public void addListener(ChangeListener<? super String> listener) {
        changeListeners.add(listener);
    }

    public void removeListener(ChangeListener<? super String> listener) {
        changeListeners.remove(listener);
    }

    /**
     * Constructs the "value" of this {@link ObservableValue}, i.e. the query string.
     *
     * @return The query string.
     */
    public String getValue() {
        return searchParameters.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&"));
    }

    public SimpleStringProperty titleProperty() {
        return title;
    }

    public SimpleStringProperty textProperty() {
        return text;
    }

    public SimpleStringProperty authorProperty() {
        return author;
    }

    public SimpleStringProperty levelProperty() {
        return level;
    }

    public SimpleStringProperty tagsProperty() {
        return tags;
    }

    public SimpleStringProperty logbooksProperty() {
        return logbooks;
    }

    public SimpleStringProperty startTimeProperty() {
        return startTime;
    }

    public SimpleStringProperty endTimeProperty() {
        return endTime;
    }

    public SimpleStringProperty attachmentsProperty() {
        return attachments;
    }

    /**
     * Called to set a new query from user input, i.e. UI input (editable ComboBox).
     *
     * @param query A query string parsed that only supported query parameters are considered.
     */
    public void setQuery(String query) {
        Map<String, String> map = LogbookQueryUtil.parseHumanReadableQueryString(query);
        searchParameters.clear();
        map.forEach((key1, value) -> {
            Keys key = Keys.findKey(key1);
            if (key != null) {
                searchParameters.put(key, value.trim());
            }
        });
        if (map.containsKey(Keys.TITLE.getName())) {
            titleProperty().setValue(Keys.TITLE.getName());
        } else {
            titleProperty().setValue(null);
        }
        if (map.containsKey(Keys.OWNER.getName())) {
            authorProperty().setValue(map.get(Keys.OWNER.getName()));
        } else {
            authorProperty().setValue(null);
        }
        if (map.containsKey(Keys.DESC.getName())) {
            textProperty().setValue(map.get(Keys.DESC.getName()));
        } else {
            textProperty().setValue(null);
        }
        if (map.containsKey(Keys.LEVEL.getName())) {
            levelProperty().setValue(map.get(Keys.LEVEL.getName()));
        } else {
            levelProperty().setValue(null);
        }
        if (map.containsKey(Keys.TAGS.getName())) {
            tagsProperty().setValue(map.get(Keys.TAGS.getName()));
        } else {
            tagsProperty().setValue(null);
        }
        if (map.containsKey(Keys.LOGBOOKS.getName())) {
            logbooksProperty().setValue(map.get(Keys.LOGBOOKS.getName()));
        } else {
            logbooksProperty().setValue(null);
        }
        if (map.containsKey(Keys.STARTTIME.getName())) {
            startTimeProperty().setValue(map.get(Keys.STARTTIME.getName()));
        } else {
            startTimeProperty().setValue(null);
        }
        if (map.containsKey(Keys.ENDTIME.getName())) {
            endTimeProperty().setValue(map.get(Keys.ENDTIME.getName()));
        } else {
            endTimeProperty().setValue(null);
        }
    }

    private void notifyListeners() {
        for (InvalidationListener listener : invalidationListeners) {
            listener.invalidated(this);
        }

        for (ChangeListener<? super String> listener : changeListeners) {
            listener.changed(this, null, getValue());
        }
    }
}
