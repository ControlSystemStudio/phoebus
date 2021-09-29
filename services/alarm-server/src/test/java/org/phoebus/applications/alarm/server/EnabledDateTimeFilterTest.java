package org.phoebus.applications.alarm.server;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

/** JUnit test of the {@link EnabledDateTimeFilter}
 *  @author Jacqueline Garrahan
 */
@SuppressWarnings("nls")
public class EnabledDateTimeFilterTest
{
    private AtomicInteger updates = new AtomicInteger();

    // Start with is_enabled false
    private boolean is_enabled = false;
    public static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Before
    public void setup()
    {
        // Configure logging to show 'all'
        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.ALL);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(Level.ALL);

        // Disable some messages
        Logger.getLogger("com.cosylab.epics").setLevel(Level.SEVERE);
    }

    public void filterChanged(final boolean enabled)
    {
        System.err.println("Enabled date sets to " + String.valueOf(enabled));
        updates.incrementAndGet();
        synchronized (this)
        {
            is_enabled  = enabled;
            notifyAll();
        }
    }

    @Test(timeout=8000)
    public void testEnableDate() throws Exception
    {
        is_enabled = false;


        final LocalDateTime update_date = LocalDateTime.now().plusSeconds(2);
        final EnabledDateTimeFilter filter = new EnabledDateTimeFilter(update_date, this::filterChanged);

        // wait until this is marked as enabled
        synchronized (this)
        {
            while (! is_enabled)
                wait();
        }
        filter.cancel();
    }

}
