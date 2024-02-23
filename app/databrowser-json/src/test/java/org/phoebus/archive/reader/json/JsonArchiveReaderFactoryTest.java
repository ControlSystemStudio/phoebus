/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link JsonArchiveReaderFactory}.
 */
public class JsonArchiveReaderFactoryTest extends HttpServerTestBase {

    /**
     * Tests the {@link JsonArchiveReaderFactory#createReader(String)} method.
     */
    @Test
    public void createReader() {
        var archive_info_json = """
                [ {
                  "key" : 1,
                  "name" : "",
                  "description" : "Dummy archive"
                } ]
                """;
        withArchiveInfo(archive_info_json, (base_url) -> {
            try {
                assertEquals(
                        "Dummy archive",
                        new JsonArchiveReaderFactory()
                                .createReader("json:" + base_url)
                                .getDescription());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Tests the {@link JsonArchiveReaderFactory#getPrefix()} method.
     */
    @Test
    public void getPrefix() {
        assertEquals("json", new JsonArchiveReaderFactory().getPrefix());
    }

}
