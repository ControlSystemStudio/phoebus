package org.phoebus.pv.archive.retrieve;

import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.pv.PV;
import org.phoebus.pv.archive.ArchiveReaderService;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A Connection to a PV in the archiver
 *
 * @author Kunal Shroff
 */
public class ArchivePV extends PV {

    ArchiveReaderService service = ArchiveReaderService.getService();

    public ArchivePV(String name) {
        this(name, Instant.now());
    }

    public ArchivePV(String name, Instant instant) {
        super(name);
        try {
            ValueIterator i = service.getReader().getRawValues(name, instant, instant);

            if (i.hasNext()) {
                notifyListenersOfValue(i.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnected() {
        notifyListenersOfDisconnect();
    }

    @Override
    protected void close() {
        super.close();
    }

    @Override
    public boolean isReadonly() {
        return true;
    }

}
