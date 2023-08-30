package org.phoebus.applications.eslog.archivedjmslog;

import java.util.Arrays;

import org.phoebus.applications.eslog.Helpers;
import org.phoebus.applications.eslog.model.EventLogMessage;

public class MessageSeverityPropertyFilter extends StringPropertyMultiFilter
{
    protected final static String MSG_PROPERTY = EventLogMessage.SEVERITY;
    protected int minLevel;

    public MessageSeverityPropertyFilter(final int minLevel)
    {
        super(MessageSeverityPropertyFilter.MSG_PROPERTY,
                Arrays.copyOfRange(Helpers.LOG_LEVELS, minLevel,
                        Helpers.LOG_LEVELS.length),
                false);
        this.minLevel = minLevel;
    }

    public int getMinLevel()
    {
        return this.minLevel;
    }

    @Override
    public String toString()
    {
        return MessageSeverityPropertyFilter.MSG_PROPERTY + " >= " //$NON-NLS-1$
                + Helpers.LOG_LEVELS[this.minLevel];
    }
}
