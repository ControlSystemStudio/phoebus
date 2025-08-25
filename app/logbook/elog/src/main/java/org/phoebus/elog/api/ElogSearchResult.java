package org.phoebus.elog.api;

import java.util.List;

public class ElogSearchResult {

    private int hitCount;
    private List<ElogEntry> logs;

    private ElogSearchResult(List<ElogEntry> logs, int hitCount) {
        this.logs = logs;
        this.hitCount = hitCount;
    }

    public static ElogSearchResult of(List<ElogEntry> logs, int hits) {
        return new ElogSearchResult(logs, hits);
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public List<ElogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<ElogEntry> logs) {
        this.logs = logs;
    }
}
