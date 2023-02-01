package org.phoebus.applications.eslog.archivedjmslog;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.phoebus.applications.eslog.Activator;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Merges archived data with live data from JMS. */
public class MergedModel<T extends LogMessage>
        implements ArchiveModelListener<T>, LiveModelListener<T>
{
    protected ArchiveModel<T> archive;
    protected LiveModel<T> live;
    protected ObservableList<T> messages = FXCollections.observableArrayList();
    protected List<Runnable> messagesChangedListeners = new LinkedList<>();
    protected List<Consumer<TimeRelativeInterval>> timeChangedListeners = new LinkedList<>();
    protected TimeRelativeInterval time_range = TimeRelativeInterval
            .startsAt(Duration.ofHours(8));

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

    public void addChangeListener(final Runnable r)
    {
        synchronized (this.messagesChangedListeners)
        {
            this.messagesChangedListeners.add(r);
        }
    }

    public void addTimeChangeListener(final Consumer<TimeRelativeInterval> c)
    {
        synchronized (this.timeChangedListeners)
        {
            this.timeChangedListeners.add(c);
        }
    }

    protected void expireMessages()
    {
        Instant cutoff;
        try
        {
            cutoff = this.time_range.toAbsoluteInterval().getStart();
        }
        catch (IllegalArgumentException e)
        {
            // the error has been logged, and there is nothing more we can do
            // about it.
            return;
        }
        Activator.logger
                .finer(String.format("Expiring from %s", cutoff.toString())); //$NON-NLS-1$

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
        if (expired > 0)
        {
            Activator.logger
                    .fine(String.format("%d messages expired.", expired)); //$NON-NLS-1$
        }
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

    public TimeRelativeInterval getTimerange()
    {
        return this.time_range;
    }

    public String[] getTimerangeText()
    {
        final var start = this.time_range.isStartAbsolute()
                ? TimestampFormats.MILLI_FORMAT
                        .format(this.time_range.getAbsoluteStart().get())
                : TimeParser.format(this.time_range.getRelativeStart().get());
        final var end = this.time_range.isEndAbsolute()
                ? TimestampFormats.MILLI_FORMAT
                        .format(this.time_range.getAbsoluteEnd().get())
                : TimeParser.format(this.time_range.getRelativeEnd().get());
        return new String[] { start, end };
    }

    public boolean isNowMode()
    {
        return (!this.time_range.isEndAbsolute())
                && Duration.ZERO.equals(this.time_range.getRelativeEnd().get());
    }

    @Override
    public void messagesRetrieved(ArchiveModel<T> model)
    {
        this.messages.addAll(Arrays.asList(model.getMessages()));
        notifyListeners();
    }

    @Override
    public void newMessage(T msg)
    {
        // ignore the message if we are not in "NOW" mode.
        if (!isNowMode())
        {
            return;
        }
        synchronized (this.messages)
        {
            this.messages.add(msg);
        }
        notifyListeners();
    }

    protected void notifyListeners()
    {
        synchronized (this.messagesChangedListeners)
        {
            this.messagesChangedListeners.forEach(Runnable::run);
        }
    }

    protected void notifyTimeChangedListeners()
    {
        synchronized (this.timeChangedListeners)
        {
            this.timeChangedListeners.forEach(l -> l.accept(this.time_range));
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

        // Instant, or Duration/Period (both derived from TemporalAmount)
        final var start = TimeParser.parseInstantOrTemporalAmount(start_spec);
        Instant start_instant = null;
        TemporalAmount start_amount = null;
        if (start instanceof Instant)
            start_instant = (Instant) start;
        else
            start_amount = (TemporalAmount) start;

        final var end = TimeParser.parseInstantOrTemporalAmount(end_spec);
        Instant end_instant = null;
        TemporalAmount end_amount = null;
        if (end instanceof Instant)
            end_instant = (Instant) end;
        else
            end_amount = (TemporalAmount) end;

        if ((null != start_instant) && (null != end_instant))
            this.time_range = TimeRelativeInterval.of(start_instant,
                    end_instant);
        if ((null != start_instant) && (null == end_instant))
            this.time_range = TimeRelativeInterval.of(start_instant,
                    end_amount);
        if ((null == start_instant) && (null != end_instant))
            this.time_range = TimeRelativeInterval.of(start_amount,
                    end_instant);
        if ((null == start_instant) && (null == end_instant))
            this.time_range = TimeRelativeInterval.of(start_amount, end_amount);
        notifyTimeChangedListeners();
        updateFromArchive();
    }

    public void setTimerange(final TimeRelativeInterval interval)
    {
        Activator.checkParameter(interval, "interval"); //$NON-NLS-1$
        this.time_range = interval;
        notifyTimeChangedListeners();
        updateFromArchive();
    }

    /**
     * Trigger an update from the archive.
     */
    public void updateFromArchive()
    {
        this.messages.clear();
        if (null != this.live)
        {
            if (isNowMode())
            {
                this.live.start();
            }
            else
            {
                this.live.stop();
            }
        }
        if (null != this.archive)
        {
            this.archive.refresh(this.time_range.toAbsoluteInterval());
        }
    }
}
