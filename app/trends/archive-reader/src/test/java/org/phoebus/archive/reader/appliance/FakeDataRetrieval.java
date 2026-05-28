package org.phoebus.archive.reader.appliance;

import org.epics.archiverappliance.retrieval.client.DataRetrieval;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete DataRetrieval subclass for testing.
 *
 * All three getDataForPV overloads are final and delegate to getDataForPVs,
 * so overriding getDataForPVs intercepts every call. PV names are recorded in
 * pvsCalled; responses can be stubbed by PV-name substring via whenPvContains().
 */
class FakeDataRetrieval extends DataRetrieval {

    final List<String> pvsCalled = new ArrayList<>();

    private GenMsgIterator defaultResponse;
    private final Map<String, GenMsgIterator> responses = new LinkedHashMap<>();

    FakeDataRetrieval(GenMsgIterator defaultResponse) {
        this.defaultResponse = defaultResponse;
    }

    /** Return {@code response} whenever the requested PV name contains {@code substring}. */
    void whenPvContains(String substring, GenMsgIterator response) {
        responses.put(substring, response);
    }

    @Override
    public GenMsgIterator getDataForPVs(List<String> pvNames, Timestamp start, Timestamp end,
                                        boolean fetchLatestMetadata, Map<String, String> otherParams) {
        String pv = pvNames.isEmpty() ? "" : pvNames.get(0);
        pvsCalled.add(pv);
        for (Map.Entry<String, GenMsgIterator> entry : responses.entrySet()) {
            if (pv.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return defaultResponse;
    }
}
