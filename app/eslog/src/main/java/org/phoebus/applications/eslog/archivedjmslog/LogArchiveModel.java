package org.phoebus.applications.eslog.archivedjmslog;

import java.time.Instant;

import org.phoebus.applications.eslog.model.EventLogMessage;
import org.phoebus.util.time.TimeParser;

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
        if (!TimeParser.NOW.equals(this.endSpec))
        {
            return;
        }
        synchronized (this.messages)
        {
            if (!this.messages.isEmpty())
            {
                Instant lastTime = this.messages.get(0).getTime();
                Instant thisTime = msg.getTime();
                msg.setDelta(lastTime.toEpochMilli() - thisTime.toEpochMilli());
            }
            this.messages.add(0, msg);
        }
    }
}
