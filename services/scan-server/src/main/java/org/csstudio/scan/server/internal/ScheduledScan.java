package org.csstudio.scan.server.internal;

import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.device.DeviceContext;
import org.phoebus.util.time.TimestampFormats;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;

public class ScheduledScan extends ExecutableScan {

    /**
    * Time at which this scan should be queued / executed.
    * */
    private final LocalDateTime when;

    /**
     * Used to store the point in time in which this scan was created (for showing progress)
     * */
    private final LocalDateTime created;

    /**
    * Whether the scan should be queued when its time has come
    * */
    private final boolean queued;

    /**
     * Whether the scan is currently executable (must be AFTER 'when' has passed)
     */
    private boolean executable = false;

    /**
     * Initialize
     *
     * @param when            Time at which this scan should be queued / executed
     * @param queued          Whether this scan should be queued when its time has come
     * @param engine          {@link ScanEngine} that executes this scan
     * @param jython          Jython support
     * @param name            User-provided name for this scan
     * @param devices         {@link DeviceContext} to use for scan
     * @param pre_scan        Commands to execute before the 'main' section of the scan
     * @param implementations Commands to execute in this scan
     * @param post_scan       Commands to execute before the 'main' section of the scan
     * @param timeout_secs    Timeout in seconds or 0
     * @param deadline        Deadline by which scan will be aborted or <code>null</code>
     * @throws Exception on error (cannot access log, ...)
     */
    public ScheduledScan(
            LocalDateTime when,
            boolean queued,
            ScanEngine engine,
            JythonSupport jython,
            String name,
            DeviceContext devices,
            List<ScanCommandImpl<?>> pre_scan,
            List<ScanCommandImpl<?>> implementations,
            List<ScanCommandImpl<?>> post_scan,
            long timeout_secs,
            LocalDateTime deadline) throws Exception {
        super(engine, jython, name, devices, pre_scan, implementations, post_scan, timeout_secs, deadline);
        this.when = when;
        this.queued = queued;
        this.created = LocalDateTime.now();
    }

    /** Whether this scan is ready to actually be queued / executed. Basically, if the scheduled time has not passed,
     * a ScheduledScan does not count as an "actual" ExecutableScan. If the time has passed, it does count as one.
     *
     * @return True if the scheduled time has passed
     */
    public boolean getExecutable() {
        return executable;
    }

    public void setExecutable() {
        assert LocalDateTime.now().isAfter(when);
        executable = true;
    }

    /** QueueState for scheduled scans must be handled differently:
     * If the scan is not meant to be executed yet, it will ALWAYS be Queued,
     * so that it can be moved around (mainly to the top of the queue when it is
     * meant to be executed, but users may want to move it around too for some reason)
     */
    @Override
    QueueState getQueueState() {
        if (!getExecutable())
            return QueueState.Queued;
        return super.getQueueState();
    }

    @Override
    public ScanState getScanState() {
        if (!getExecutable()) {
            return ScanState.Scheduled;
        }
        return super.getScanState();
    }

    @Override
    public ScanInfo getScanInfo() {
        ScanInfo base_info = super.getScanInfo();
        if (!getExecutable()) {
            // override finish time / current command if the scan is still scheduled
            long total_duration = Duration.between(created, getScheduledTime()).toMillis();
            return new ScanInfo(
                    this,
                    base_info.getState(),
                    base_info.getError(),
                    base_info.getRuntimeMillisecs(),
                    getScheduledTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    total_duration - Duration.between(LocalDateTime.now(), getScheduledTime()).toMillis(),
                    total_duration,
                    base_info.getCurrentAddress(),
                    "Waiting until " + getScheduledTime().format(TimestampFormats.SECONDS_FORMAT) + "..."
            );
        }
        return base_info;
    }


    @Override
    void doAbort(ScanState previous) {
        // first set to executable, then abort it as usual
        setExecutable();
        super.doAbort(previous);
    }

    LocalDateTime getScheduledTime() {
        return when;
    }

    boolean getQueued() {
        return queued;
    }
}
