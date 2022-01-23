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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchParameters implements ObservableValue<String> {

    private SimpleStringProperty title = new SimpleStringProperty();
    private SimpleStringProperty body = new SimpleStringProperty();
    private SimpleStringProperty author = new SimpleStringProperty();
    private SimpleStringProperty level = new SimpleStringProperty();
    private SimpleStringProperty tags = new SimpleStringProperty();
    private SimpleStringProperty logbooks = new SimpleStringProperty();
    private SimpleStringProperty startTime = new SimpleStringProperty();
    private SimpleStringProperty endTime = new SimpleStringProperty();

    private final List<InvalidationListener> invalidListeners = new ArrayList<>();
    private final List<ChangeListener<? super String>> changeListeners = new ArrayList<>();

    public SearchParameters(){
        title.addListener((observable, oldValue, newValue) -> notifyListeners());
    }

    public void addListener(InvalidationListener listener) {
        invalidListeners.add(listener);
    }

    public void removeListener(InvalidationListener listener) {
        invalidListeners.remove(listener);
    }

    public void addListener(ChangeListener<? super String> listener) {
        changeListeners.add(listener);
    }

    public void removeListener(ChangeListener<? super String> listener) {
        changeListeners.remove(listener);
    }

    public String getValue() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(titleProperty().get())) {
            stringBuilder.append(Keys.TITLE.getName()).append("=").append(titleProperty().get());
        }
        return stringBuilder.toString();
    }

    public String getTitle() {
        return title.get();
    }

    public SimpleStringProperty titleProperty() {
        return title;
    }


    public void setTitle(String title) {
        this.title.setValue(title);
        notifyListeners();
    }


    public String getBody() {
        return body.get();
    }

    public SimpleStringProperty bodyProperty() {
        return body;
    }

    public void setBody(String body) {
        this.body.set(body);
    }

    public String getAuthor() {
        return author.get();
    }

    public SimpleStringProperty authorProperty() {
        return author;
    }

    public void setAuthor(String author) {
        this.author.set(author);
    }

    public String getLevel() {
        return level.get();
    }

    public SimpleStringProperty levelProperty() {
        return level;
    }

    public void setLevel(String level) {
        this.level.set(level);
    }

    public String getTags() {
        return tags.get();
    }

    public SimpleStringProperty tagsProperty() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags.set(tags);
    }

    public String getLogbooks() {
        return logbooks.get();
    }

    public SimpleStringProperty logbooksProperty() {
        return logbooks;
    }

    public void setLogbooks(String logbooks) {
        this.logbooks.set(logbooks);
    }

    public String getStartTime() {
        return startTime.get();
    }

    public SimpleStringProperty startTimeProperty() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime.set(startTime);
    }

    public String getEndTime() {
        return endTime.get();
    }

    public SimpleStringProperty endTimeProperty() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime.set(endTime);
    }

    public void setQuery(String query) {
        Map<String, String> map = LogbookQueryUtil.parseHumanReadableQueryString(query);
        if (map.containsKey(Keys.TITLE.getName())) {
            title.setValue(map.get(Keys.TITLE.getName()));
        } else {
            title.setValue(null);
        }
        if (map.containsKey(Keys.AUTHOR.getName())) {
            author.setValue(map.get(Keys.AUTHOR.getName()));
        } else {
            author.setValue(null);
        }
        if (map.containsKey(Keys.SEARCH.getName())) {
            body.setValue(map.get(Keys.SEARCH.getName()));
        } else {
            body.setValue(null);
        }
        if (map.containsKey(Keys.LEVEL.getName())) {
            level.setValue(map.get(Keys.LEVEL.getName()));
        } else {
            level.setValue(null);
        }
        if (map.containsKey(Keys.TAGS.getName())) {
            tags.setValue(map.get(Keys.TAGS.getName()));
        } else {
            tags.setValue(null);
        }
        if (map.containsKey(Keys.LOGBOOKS.getName())) {
            logbooks.setValue(map.get(Keys.LOGBOOKS.getName()));
        } else {
            logbooks.setValue(null);
        }
        if (map.containsKey(Keys.STARTTIME.getName())) {
            startTime.setValue(map.get(Keys.STARTTIME.getName()));
        } else {
            startTime.setValue(null);
        }
        if (map.containsKey(Keys.ENDTIME.getName())) {
            endTime.setValue(map.get(Keys.ENDTIME.getName()));
        } else {
            endTime.setValue(null);
        }
    }

    private void notifyListeners()
    {
        for (InvalidationListener listener : invalidListeners)
            listener.invalidated(this);
        for (ChangeListener<? super String> listener : changeListeners)
            listener.changed(this, null, getValue());
    }
}
