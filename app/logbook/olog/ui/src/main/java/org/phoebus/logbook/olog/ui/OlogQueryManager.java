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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class OlogQueryManager {

    private static OlogQueryManager INSTANCE;
    private static final int QUERY_LIST_SIZE = 20;
    private List<OlogQuery> ologQueries;
    private Comparator<OlogQuery> ologQueryComparator
            = Comparator.comparing(OlogQuery::getLastUsed).reversed();

    private OlogQueryManager() {
        ologQueries = new ArrayList<>(QUERY_LIST_SIZE);
    }

    public static OlogQueryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new OlogQueryManager();
        }
        return INSTANCE;
    }

    public void setQueries(List<OlogQuery> queries) {
        ologQueries = queries;
    }

    public List<OlogQuery> getQueries() {
        return ologQueries;
    }

    public OlogQuery getOrAddQuery(String queryString) {
        Optional<OlogQuery> ologQuery =
                ologQueries.stream().filter(q -> q.getQuery().equals(queryString)).findFirst();
        if (ologQuery.isPresent()) {
            // Update last used date!
            ologQuery.get().setLastUsed(Instant.now());
            return ologQuery.get();
        }
        OlogQuery newQuery = new OlogQuery(queryString);
        if (ologQueries.size() == QUERY_LIST_SIZE) {
            // Find oldest query that is not the default query and flush it out.
            OlogQuery queryToFlush = null;
            for (int i = ologQueries.size() - 1; i >= 0; i--) {
                OlogQuery query = ologQueries.get(i);
                if (!query.isDefaultQuery()) {
                    queryToFlush = query;
                    break;
                }
            }
            if (queryToFlush != null) {
                ologQueries.remove(queryToFlush);
            }
        }
        ologQueries.add(newQuery);
        ologQueries.sort(ologQueryComparator);
        return newQuery;
    }
}
