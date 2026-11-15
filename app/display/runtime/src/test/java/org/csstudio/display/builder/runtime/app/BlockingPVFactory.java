package org.csstudio.display.builder.runtime.app;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

/** Creates PVs "blocking://..." for tests
 *
 *  <p>After {@link #block()}, creating the next PV waits until {@link #release()},
 *  like a PV type that connects while it is created.
 *  This keeps a display runtime in its start for as long as a test needs.
 */
@SuppressWarnings("nls")
public class BlockingPVFactory implements PVFactory
{
    private static volatile CountDownLatch creating = new CountDownLatch(0);
    private static volatile CountDownLatch released = new CountDownLatch(0);

    /** Make the next PV creation wait until {@link #release()} */
    static void block()
    {
        creating = new CountDownLatch(1);
        released = new CountDownLatch(1);
    }

    /** Wait until a PV creation is waiting
     *  @throws InterruptedException when interrupted
     */
    static void awaitCreation() throws InterruptedException
    {
        if (! creating.await(10, TimeUnit.SECONDS))
            throw new IllegalStateException("No PV was created");
    }

    /** Let a waiting PV creation continue */
    static void release()
    {
        released.countDown();
    }

    @Override
    public String getType()
    {
        return "blocking";
    }

    @Override
    public PV createPV(final String name, final String base_name) throws Exception
    {
        creating.countDown();
        if (! released.await(10, TimeUnit.SECONDS))
            throw new IllegalStateException("PV creation was not released");
        return new PV(name)
        {
            // Never connects, which is enough for these tests
        };
    }
}
