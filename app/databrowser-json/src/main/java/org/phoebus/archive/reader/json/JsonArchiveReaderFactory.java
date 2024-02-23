/*******************************************************************************
 * Copyright (c) 2013-2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.json;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/**
 * <p>
 * Factory for {@link JsonArchiveReader} instances. This type of archive reader
 * handles archive URLs starting with <code>json:</code> and implements the
 * <a href="https://oss.aquenos.com/cassandra-pv-archiver/docs/3.2.6/manual/html/apb.html">
 * JSON archive access protocol 1.0</a>.
 * </p>
 *
 * <p>
 * Instances of this class are thread-safe.
 * </p>
 */
public class JsonArchiveReaderFactory implements ArchiveReaderFactory {

    @Override
    public ArchiveReader createReader(String url) throws Exception {
        if (!url.startsWith("json:")) {
            throw new IllegalArgumentException(
                    "URL must start with scheme \"json:\".");
        }
        return new JsonArchiveReader(
                url, JsonArchivePreferences.getDefaultInstance());
    }

    @Override
    public String getPrefix() {
        return "json";
    }

}
