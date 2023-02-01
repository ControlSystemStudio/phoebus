package org.phoebus.applications.eslog.archivedjmslog;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.eslog.Activator;
import org.phoebus.util.time.TimeParser;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Merges archived data with live data from JMS. */
public class MergedModel<T extends LogMessage>
        implements ArchiveModelListener<T>, LiveModelListener<T>
{
    protected ArchiveModel<T> archive;
    protected LiveModel<T> live;
    protected ObservableList<T> messages = FXCollections.observableArrayList();
    protected String startSpec = "-8 hour"; //$NON-NLS-1$
    protected String endSpec = TimeParser.NOW;

    protected ScheduledExecutorService expireService;

    protected PropertyFilter[] filters;

    private Class<T> parameterType;

    @SuppressWarnings("unchecked")
    public MergedModel(ArchiveModel<T> archive, LiveModel<T> live)
    {
        this.archive = archive;
        if (null != this.archive)
        {
            this.archive.addListener(this);
        }
        this.live = live;
        if (null != this.live)
        {
            this.live.addListener(this);
        }

        this.expireService = Executors.newSingleThreadScheduledExecutor();
        this.expireService.scheduleAtFixedRate(this::expireMessages, 1, 1,
                TimeUnit.MINUTES);

        this.parameterType = ((Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    protected void expireMessages()
    {
        Instant cutoff;
        try
        {
            cutoff = timeSpecToInstant(this.startSpec);
        }
        catch (IllegalArgumentException e)
        {
            // the error has been logged, and there is nothing more we can do
            // about it.
            return;
        }
        Activator.logger
                .fine(String.format("Expiring from %s", cutoff.toString())); //$NON-NLS-1$

        long expired = 0L;
        synchronized (this.messages)
        {
            Iterator<T> i = this.messages.iterator();
            while (i.hasNext())
            {
                LogMessage msg = i.next();
                if (cutoff.compareTo(msg.getTime()) < 0)
                {
                    // the first message with a timestamp > cutoff.
                    // no need to look any further.
                    break;
                }
                i.remove();
                ++expired;
            }
        }
        Activator.logger.fine(String.format("%d messages expired.", expired)); //$NON-NLS-1$
    }

    public String getEndSpec()
    {
        return this.endSpec;
    }

    public PropertyFilter[] getFilters()
    {
        return this.filters;
    }

    protected void getFromArchive(Instant from, Instant to)
    {
        this.archive.refresh(from, to);
    }

    @SuppressWarnings("unchecked")
    public T[] getMessages()
    {
        synchronized (this.messages)
        {
            return this.messages.toArray((T[]) Array
                    .newInstance(this.parameterType, this.messages.size()));
        }
    }

    public String getStartSpec()
    {
        return this.startSpec;
    }

    @Override
    public void messagesRetrieved(ArchiveModel<T> model)
    {
        this.messages.addAll(Arrays.asList(model.getMessages()));
    }

    @Override
    public void newMessage(T msg)
    {
        // ignore the message if we are not in "NOW" mode.
        if (!TimeParser.NOW.equals(this.endSpec))
        {
            return;
        }
        synchronized (this.messages)
        {
            this.messages.add(msg);
        }
    }

    /**
     * Define the filters to use when receiving messages.
     *
     * The model will be updates from the archive.
     *
     * @param filters
     *            The new filter definitions.
     */
    public void setFilters(PropertyFilter[] filters)
    {
        this.filters = filters;
        if (null != this.live)
        {
            this.live.setFilters(filters);
        }
        if (null != this.archive)
        {
            this.archive.setFilters(filters);
        }
        updateFromArchive();
    }

    /**
     * Define the time interval to represent in the model.
     *
     * @param start_spec
     *            The start time.
     * @param end_spec
     *            The end time. Set to {@value RelativeTime#NOW} to enable the
     *            reception of live messages via JMS.
     */
    public void setTimerange(final String start_spec, final String end_spec)
            throws IllegalArgumentException
    {
        Activator.checkParameterString(start_spec, "start_spec"); //$NON-NLS-1$
        Activator.checkParameterString(end_spec, "end_spec"); //$NON-NLS-1$

        // parse to detect invalid strings.
        timeSpecToInstant(start_spec);
        timeSpecToInstant(end_spec);

        // only then assign to our local variable.
        this.startSpec = start_spec;
        this.endSpec = end_spec;
        updateFromArchive();
    }

    static Instant timeSpecToInstant(final String spec)
            throws IllegalArgumentException
    {
        var parsed = TimeParser.parseInstantOrTemporalAmount(spec);
        if ((null == parsed) || (parsed.equals(Duration.ZERO)
                && !TimeParser.NOW.equals(spec)))
        {
            var errorText = String.format(
                    "Time specification cannot be parsed: %s", //$NON-NLS-1$
                    spec);
            Activator.logger.info(errorText);
            throw new IllegalArgumentException(errorText);
        }

        if (parsed instanceof Instant) return (Instant) parsed;

        // TODO: why does the "-" get lost in the first place?
        if (spec.startsWith("-")) //$NON-NLS-1$
            return Instant.now().minus((TemporalAmount) parsed);
        else
            return Instant.now().plus((TemporalAmount) parsed);
    }

    /**
     * Trigger an update from the archive.
     */
    public void updateFromArchive()
    {
        this.messages.clear();
        if (null != this.live)
        {
            if (TimeParser.NOW.equals(this.endSpec))
                this.live.start();
            else
                this.live.stop();
        }
        if (null != this.archive)
        {
            this.archive.refresh(timeSpecToInstant(this.startSpec),
                    timeSpecToInstant(this.endSpec));
        }
    }
}
