package org.phoebus.archive.reader.appliance;

import org.epics.archiverappliance.retrieval.client.DataRetrieval;

/**
 * ApplianceArchiveReader that injects a provided DataRetrieval, avoiding any real network calls.
 */
class FakeApplianceArchiveReader extends ApplianceArchiveReader {

    private final DataRetrieval retrieval;

    FakeApplianceArchiveReader(DataRetrieval retrieval) {
        super("pbraw://fake-host:17668", false, true);
        this.retrieval = retrieval;
    }

    FakeApplianceArchiveReader(DataRetrieval retrieval, boolean useStatistics, boolean useNewOptimizedOperator) {
        super("pbraw://fake-host:17668", useStatistics, useNewOptimizedOperator);
        this.retrieval = retrieval;
    }

    @Override
    public DataRetrieval createDataRetriveal(String url) {
        return retrieval;
    }
}
