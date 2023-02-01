package org.phoebus.applications.eslog.archivedjmslog;

import java.time.Instant;

import org.phoebus.applications.eslog.model.EventLogMessage;

import javafx.collections.ObservableList;

public class LogArchiveModel extends MergedModel<EventLogMessage>
{
    public LogArchiveModel(ArchiveModel<EventLogMessage> archive,
            LiveModel<EventLogMessage> live)
    {
        super(archive, live);
    }

    public ObservableList<EventLogMessage> getObservable()
    {
        return this.messages;
    }

    @Override
    public void newMessage(EventLogMessage msg)
    {
        // ignore the message if we are not in "NOW" mode.
        if (!isNowMode())
        {
            return;
        }
        synchronized (this.messages)
        {
            if (!this.messages.isEmpty())
            {
                // we cannot rely on the sort order of messages. Sorting the
                // table will sort this list!
                final var lastTime = this.messages.parallelStream()
                        .map(EventLogMessage::getTime)
                        // reverse the sort order
                        .sorted((a, b) -> b.compareTo(a)).findFirst().get();
                Instant thisTime = msg.getTime();
                msg.setDelta(thisTime.toEpochMilli() - lastTime.toEpochMilli());
            }
            this.messages.add(msg);
        }
        notifyListeners();
    }
}