package org.phoebus.pv.archive;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.appliance.ApplianceArchiveReader;

public class ArchiveReaderService {

    /**
     * Singleton
     */
    private static final ArchiveReaderService INSTANCE = new ArchiveReaderService();

    private final ArchiveReader reader;

    public static ArchiveReaderService getService() {
        return INSTANCE;
    }

    private ArchiveReaderService() {
        // Might have to add support for multiple AA URL's
        reader = createReader(Preferences.archive_url);
    }

    private ArchiveReader createReader(final String url) {
        final ApplianceArchiveReader reader = new ApplianceArchiveReader(url, false, true);
        return reader;
    }

    public ArchiveReader getReader() {
        return reader;
    }
}
