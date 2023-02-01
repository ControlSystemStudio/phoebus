package org.phoebus.applications.eslog.archivedjmslog;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.phoebus.applications.eslog.Activator;
import org.phoebus.util.time.TimeInterval;

/** Model representing data in an archive, typically some sort of database. */
public abstract class ArchiveModel<T extends LogMessage> extends Model
{
    private final Set<ArchiveModelListener<T>> listeners = Collections
            .newSetFromMap(new WeakHashMap<ArchiveModelListener<T>, Boolean>());

    public void addListener(ArchiveModelListener<T> listener)
    {
        Activator.checkParameter(listener, "listener"); //$NON-NLS-1$
        synchronized (this.listeners)
        {
            this.listeners.add(listener);
        }
    }

    public abstract T[] getMessages();

    /**
     * Retrieve archived data from the given time interval.
     *
     * On completion of the query, the caller will be notified via the
     * {@link ArchiveModelListener#messagesRetrieved(ArchiveModel)} callback.
     *
     * @param from
     *            The start time.
     * @param to
     *            The end time.
     */
    public abstract void refresh(Instant from, Instant to);

    void refresh(TimeInterval interval) {
        refresh(interval.getStart(), interval.getEnd());
    }

    public void removeListener(ArchiveModelListener<T> listener)
    {
        synchronized (this.listeners)
        {
            this.listeners.remove(listener);
        }
    }

    protected void sendCompletionNotification()
    {
        synchronized (this.listeners)
        {
            this.listeners.forEach((r) -> {
                try
                {
                    r.messagesRetrieved(this);
                }
                catch (Throwable ex)
                {
                    Activator.logger
                            .warning("Notification failed: " + ex.getMessage()); //$NON-NLS-1$
                    ex.printStackTrace();
                }
            });
        }
    }
}
