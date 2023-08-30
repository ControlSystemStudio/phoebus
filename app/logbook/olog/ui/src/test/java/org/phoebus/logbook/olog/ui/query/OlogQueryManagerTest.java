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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OlogQueryManagerTest {

    private static File file;
    private static File nonEmptyFile;

    @BeforeAll
    public static void before() {
        try {
            file = File.createTempFile("test", "");
            file.deleteOnExit();
            nonEmptyFile = File.createTempFile("test", "");
            nonEmptyFile.deleteOnExit();
            FileOutputStream fileOutputStream = new FileOutputStream(nonEmptyFile);
            fileOutputStream.write("[{\"lastUsed\":1642665327171,\"query\":\"start=12 weeks&end=now\",\"defaultQuery\":true}]".getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetOrAddQuery() {
        OlogQueryManager ologQueryManager = OlogQueryManager.getInstance(file);
        OlogQuery defaultQuery = new OlogQuery("default");
        defaultQuery.setDefaultQuery(true);
        List<OlogQuery> queryList = new ArrayList<>();
        queryList.add(defaultQuery);
        ologQueryManager.setQueries(queryList);

        ologQueryManager.getOrAddQuery("another query");

        assertEquals(2, ologQueryManager.getQueries().size());

        for (int i = 0; i < 30; i++) {
            ologQueryManager.getOrAddQuery("index " + i);
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertEquals(15, ologQueryManager.getQueries().size());

        queryList = ologQueryManager.getQueries();
        OlogQuery oq1 = queryList.get(queryList.size() - 1);
        OlogQuery oq2 = queryList.get(queryList.size() - 2);
        assertTrue(oq1.getLastUsed() < oq2.getLastUsed());

        // Check that default query is present
        assertTrue(queryList.stream().anyMatch(OlogQuery::isDefaultQuery));
    }

    @Test
    public void testGetOrAddQueryExisting() {
        OlogQueryManager ologQueryManager = OlogQueryManager.getInstance(file);
        OlogQuery retrievedQuery = ologQueryManager.getOrAddQuery("query");
        long lastUsed = retrievedQuery.getLastUsed();

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ologQueryManager.getOrAddQuery("query");
        assertTrue(retrievedQuery.getLastUsed() > lastUsed);
    }

    @Test
    public void testNonEmtpyFile() {
        OlogQueryManager ologQueryManager = OlogQueryManager.getInstance(nonEmptyFile);
        List<OlogQuery> queries = ologQueryManager.getQueries();
        assertEquals(1, queries.size());
    }
}
