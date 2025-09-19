/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.archive.reader.influx2;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/**
 * SPI Factory for "influx:" archive URLs.
 * This factory is discovered via Java's Service Provider Interface (SPI) and is responsible
 * for creating the InfluxArchiveReader when a DataBrowser URL with the "influx:" scheme is used.
 */
public class InfluxArchiveReaderFactory implements ArchiveReaderFactory {
    /** Unique prefix that identifies InfluxDB data source URLs */
    public static final String PREFIX = "influx2:";

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public ArchiveReader createReader(final String url) throws Exception {
        return new InfluxArchiveReader(url);
    }
}
