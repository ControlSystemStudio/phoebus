package org.epics.pva.client;

import org.epics.pva.data.PVABool;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;

import java.util.ArrayList;
import java.util.List;

public class RecordOptions {
    private static final String STRUCTURE_RECORD_NAME = "record";
    private static final String STRUCTURE_OPTIONS_NAME = "_options";

    public static final RecordOptions DEFAULT = new RecordOptions(false, 0, null);
    private final boolean completion;
    private final int pipeline;
    private final DBEMask dbeMask;

    /** DBE mask to use for monitoring
     * */
    public enum DBEMask {
        DBE_NOTHING(0),
        DBE_VALUE(1),
        DBE_ARCHIVE(2),
        DBE_ALARM(4);

        private final int mask;
        DBEMask(final int mask) { this.mask = mask; }
        public int getMask() { return mask; }
    }

    private RecordOptions(boolean completion, int pipeline, DBEMask dbeMask) {
        if (pipeline > 0  &&  completion)
            throw new IllegalStateException("Cannot use both 'pipeline' (for get) " +
                "and 'completion' (for put) within same request");

        this.completion = completion;
        this.pipeline = pipeline;
        this.dbeMask = dbeMask;

    }
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean completion;
        private int pipeline;
        private DBEMask dbeMask = null;
        public Builder() {
        }

        Builder  completion(boolean completion) {
            this.completion = completion;
            return this;
        }
        Builder pipeline(int pipeline) {
            this.pipeline = pipeline;
            return this;
        }
        Builder dbeMask(DBEMask dbeMask) {
            this.dbeMask = dbeMask;
            return this;
        }
        public RecordOptions build() {
            return new RecordOptions(completion, pipeline, dbeMask);
        }
    }

    public boolean completion() { return completion; }
    public int pipeline() { return pipeline; }
    public DBEMask dbeMask() { return dbeMask; }

    public List<PVAData> structureItems() {
        List<PVAData> items = new ArrayList<>();

        if (pipeline > 0) {
            // record._options.pipeline=true
            // 'pvmonitor' encodes as PVAString 'true', not PVABool
            items.add(
                new PVAStructure(STRUCTURE_RECORD_NAME, "",
                    new PVAStructure(STRUCTURE_OPTIONS_NAME, "",
                        new PVABool("pipeline", true),
                        new PVAInt("queueSize", pipeline)
                    )));
        } else if (completion) {
            // Similar to Channel Access put-callback:
            // Process passive record (could also use "true" to always process),
            // then block until processing completes
            // record._options.process="passive"
            // record._options.block=true
            items.add(
                new PVAStructure(STRUCTURE_RECORD_NAME, "",
                    new PVAStructure(STRUCTURE_OPTIONS_NAME, "",
                        new PVAString("process", "passive"),
                        new PVABool("block", true)
                    )));
        }
        if (dbeMask != null) {
            items.add(
                new PVAStructure(STRUCTURE_RECORD_NAME, "",
                    new PVAStructure(STRUCTURE_OPTIONS_NAME, "",
                        new PVAInt("DBE", dbeMask.getMask())
                    )));
        }
        return items;
    }
}
