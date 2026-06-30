package org.csstudio.scan.server.internal;

import java.util.TimerTask;

public class ScheduledScanTask extends TimerTask {
    final ScheduledScan scan;
    final ScanEngine engine;

    public ScheduledScanTask(ScheduledScan scan, ScanEngine engine) {
        this.scan = scan;
        this.engine = engine;
    }

    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run() {
        try {
            // scan may have been aborted
            if (!scan.getScanState().isDone()) {
                engine.startScheduled(scan);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
