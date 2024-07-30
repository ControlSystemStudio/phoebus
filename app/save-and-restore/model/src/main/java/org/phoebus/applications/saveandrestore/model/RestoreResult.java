package org.phoebus.applications.saveandrestore.model;

import java.io.Serializable;

public class RestoreResult implements Serializable {
    private SnapshotItem snapshotItem;
    private String errorMsg;

    public SnapshotItem getSnapshotItem() {
        return snapshotItem;
    }
    public String getErrorMsg() {
        return errorMsg;
    }
    public void setSnapshotItem(SnapshotItem snapshotItem) {
        this.snapshotItem = snapshotItem;
    }
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}