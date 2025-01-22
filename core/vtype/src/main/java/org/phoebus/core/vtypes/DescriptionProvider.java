package org.phoebus.core.vtypes;

public interface DescriptionProvider {

    /**
     * Human-readable description of the underlying data, e.g. the DESC field of an EPICS record.
     * @return description, or <code>null</code> if not set.
     */
    public String getDescription();
}
