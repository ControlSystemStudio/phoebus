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

package org.phoebus.applications.saveandrestore.ui.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.framework.workbench.Locations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchQueryManager {

    private static SearchQueryManager INSTANCE;
    private List<SearchQuery> searchQueries;
    private final Comparator<SearchQuery> searchQueryComparator
            = Comparator.comparing(SearchQuery::getLastUsed).reversed();
    private final ObjectMapper objectMapper =
            new ObjectMapper();
    private final File searchQueriesFile;

    private static final int QUERY_LIST_SIZE = 15;

    private SearchQueryManager(File file) {
        searchQueriesFile = file;
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        initialize();
    }

    public static SearchQueryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SearchQueryManager(new File(Locations.user(), "save_and_restore_queries"));
        }
        return INSTANCE;
    }

    public static SearchQueryManager getInstance(File file) {
        if (INSTANCE == null) {
            INSTANCE = new SearchQueryManager(file);
        }
        return INSTANCE;
    }

    private void initialize() {
        if (searchQueriesFile.exists()) {
            try {
                searchQueries = objectMapper.readValue(searchQueriesFile, new TypeReference<>() {
                });
            } catch (IOException e) {
                //e.g. empty file
                searchQueries = new ArrayList<>();
            }
        } else {
            searchQueries = new ArrayList<>();
            SearchQuery defaultQuery = new SearchQuery(Preferences.default_search_query);
            defaultQuery.setDefaultQuery(true);
            searchQueries.add(defaultQuery);
            save();
        }
    }

    public int getQueryListSize(){
        return QUERY_LIST_SIZE;
    }

    public void setQueries(List<SearchQuery> queries) {
        searchQueries = queries;
    }

    /**
     * Returns the current list of {@link SearchQuery}s. If the list has not yet been
     * initialized, it is populated form file or - if the file does not exist - the default query
     * read from properties.
     *
     * @return A list of managed {@link SearchQuery}s.
     */
    public List<SearchQuery> getQueries() {
        return searchQueries;
    }

    public SearchQuery getOrAddQuery(SearchQuery query) {
        return getOrAddQuery(query.getQuery());
    }

    public SearchQuery getOrAddQuery(String queryString) {

        Optional<SearchQuery> ologQuery =
                searchQueries.stream().filter(q -> q.getQuery().equals(queryString)).findFirst();
        SearchQuery query;
        if (ologQuery.isPresent()) {
            // Update last used date!
            ologQuery.get().setLastUsed(System.currentTimeMillis());
            query = ologQuery.get();
        } else {
            query = new SearchQuery(queryString);
            if (searchQueries.size() == 15) {
                // Find oldest query that is not the default query and flush it out.
                SearchQuery queryToFlush = null;
                for (int i = searchQueries.size() - 1; i >= 0; i--) {
                    SearchQuery q = searchQueries.get(i);
                    if (!q.isDefaultQuery()) {
                        queryToFlush = q;
                        break;
                    }
                }
                if (queryToFlush != null) {
                    searchQueries.remove(queryToFlush);
                }
            }
            searchQueries.add(query);
        }
        searchQueries.sort(searchQueryComparator);
        return query;
    }

    public void save() {
        try {
            objectMapper.writeValue(searchQueriesFile, searchQueries);
        } catch (IOException e) {
            Logger.getLogger(SearchQueryManager.class.getName())
                    .log(Level.WARNING, "Failed to save Olog queries file", e);
        }
    }
}
