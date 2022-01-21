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

package org.phoebus.logbook.olog.ui.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.logbook.olog.ui.LogbookUIPreferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OlogQueryManager {

    private static OlogQueryManager INSTANCE;
    private List<OlogQuery> ologQueries;
    private Comparator<OlogQuery> ologQueryComparator
            = Comparator.comparing(OlogQuery::getLastUsed).reversed();
    private ObjectMapper objectMapper =
            new ObjectMapper();
    private File ologQueriesFile;

    private int queryListSize = 15;

    private OlogQueryManager(File file) {
        ologQueriesFile = file;
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        initialize();
    }

    public static OlogQueryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new OlogQueryManager(new File(Locations.user(), "olog_queries"));
        }
        return INSTANCE;
    }

    public static OlogQueryManager getInstance(File file) {
        if (INSTANCE == null) {
            INSTANCE = new OlogQueryManager(file);
        }
        return INSTANCE;
    }

    private void initialize() {
        int size = LogbookUIPreferences.query_list_size;
        if(size >= 5 && size <= 30){
            queryListSize = size;
        }
        if (ologQueriesFile.exists()) {
            try {
                ologQueries = objectMapper.readValue(ologQueriesFile, new TypeReference<>() {
                });
            } catch (IOException e) {
                //e.g. empty file
                ologQueries = new ArrayList<>();
            }
        } else {
            ologQueries = new ArrayList<>();
            OlogQuery defaultQuery = new OlogQuery(LogbookUIPreferences.default_logbook_query);
            defaultQuery.setDefaultQuery(true);
            ologQueries.add(defaultQuery);
            save();
        }
    }

    public int getQueryListSize(){
        return queryListSize;
    }

    public void setQueries(List<OlogQuery> queries) {
        ologQueries = queries;
    }

    /**
     * Returns the current list of {@link OlogQuery}s. If the list has not yet been
     * initialized, it is populated form file or - if the file does not exist - the default query
     * read from properties.
     *
     * @return
     */
    public List<OlogQuery> getQueries() {
        return ologQueries;
    }

    public OlogQuery getOrAddQuery(OlogQuery query) {
        return getOrAddQuery(query.getQuery());
    }

    public OlogQuery getOrAddQuery(String queryString) {

        Optional<OlogQuery> ologQuery =
                ologQueries.stream().filter(q -> q.getQuery().equals(queryString)).findFirst();
        OlogQuery query;
        if (ologQuery.isPresent()) {
            // Update last used date!
            ologQuery.get().setLastUsed(System.currentTimeMillis());
            query = ologQuery.get();
        } else {
            query = new OlogQuery(queryString);
            if (ologQueries.size() == LogbookUIPreferences.query_list_size) {
                // Find oldest query that is not the default query and flush it out.
                OlogQuery queryToFlush = null;
                for (int i = ologQueries.size() - 1; i >= 0; i--) {
                    OlogQuery q = ologQueries.get(i);
                    if (!q.isDefaultQuery()) {
                        queryToFlush = q;
                        break;
                    }
                }
                if (queryToFlush != null) {
                    ologQueries.remove(queryToFlush);
                }
            }
            ologQueries.add(query);
        }
        ologQueries.sort(ologQueryComparator);
        return query;
    }

    public void save() {
        try {
            objectMapper.writeValue(ologQueriesFile, ologQueries);
        } catch (IOException e) {
            Logger.getLogger(OlogQueryManager.class.getName())
                    .log(Level.WARNING, "Failed to save Olog queries file", e);
        }
    }
}
