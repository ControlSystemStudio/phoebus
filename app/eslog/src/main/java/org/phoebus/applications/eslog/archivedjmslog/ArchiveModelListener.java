package org.phoebus.applications.eslog.archivedjmslog;

public interface ArchiveModelListener<T extends LogMessage>
{
    public void messagesRetrieved(ArchiveModel<T> model);
}
