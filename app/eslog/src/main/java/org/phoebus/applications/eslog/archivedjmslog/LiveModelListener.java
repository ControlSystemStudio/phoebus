package org.phoebus.applications.eslog.archivedjmslog;

public interface LiveModelListener<T extends LogMessage>
{
    public void newMessage(T msg);
}
