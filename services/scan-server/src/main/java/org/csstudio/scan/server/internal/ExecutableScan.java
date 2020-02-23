/*******************************************************************************
 * Copyright (c) 2011-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server.internal;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.csstudio.scan.command.LoopCommand;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.MemoryInfo;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandUtil;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.ScanServerInstance;
import org.csstudio.scan.server.command.WaitForDevicesCommand;
import org.csstudio.scan.server.command.WaitForDevicesCommandImpl;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.device.DeviceContext;
import org.csstudio.scan.server.device.DeviceContextHelper;
import org.csstudio.scan.server.log.DataLog;
import org.csstudio.scan.server.log.DataLogFactory;
import org.phoebus.util.time.TimestampFormats;

/** Scan that can be executed: Commands, device context, state
 *
 *  <p>Combines a {@link DeviceContext} with {@link ScanContextImpl}ementations
 *  and can execute them.
 *  When a command is executed, it receives a {@link ScanContext} view
 *  of the scan for limited access to the devices, data logger etc.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExecutableScan extends LoggedScan implements ScanContext, Callable<Object>, AutoCloseable
{
    /** Pattern for "java.lang.Exception: ", "java...Exception: " */
    private static final Pattern java_exception_pattern = Pattern.compile("java[.a-zA-Z]+Exception: ");

    enum QueueState
    {
        /** This scan is not meant to be queued */
        NotQueued,
        /** Scan is in queue */
        Queued,
        /** Scan has been submitted */
        Submitted
    };

    private volatile QueueState queue_state = QueueState.NotQueued;

    /** Engine that will execute this scan */
    private final ScanEngine engine;

    /** Jython interpreter that some commands may use.
     *  Owned by the ExecutableScan, see close()
     */
    final private JythonSupport jython;

    /** Commands to execute */
    final private transient List<ScanCommandImpl<?>> pre_scan, implementations, post_scan;

    /** Macros for resolving device names */
    final private MacroContext macros;

    /** Devices used by the scan */
    final protected DeviceContext devices;

    /** Log each device access, or require specific log command? */
    private volatile boolean automatic_log_mode = false;

    /** Data logger, present while executing the scan */
    private volatile Optional<DataLog> data_logger = Optional.empty();

    /** Total number of commands to execute */
    final private long total_work_units;

    /** Commands executed so far */
    final protected AtomicLong work_performed = new AtomicLong();

    /** State of this scan */
    private AtomicReference<ScanState> state = new AtomicReference<>(ScanState.Idle);

    /** Error message */
    private volatile Optional<String> error = Optional.empty();

    /** Start time, set when execution starts */
    private volatile long start_ms = 0;

    /** Actual or estimated end time */
    private volatile long end_ms = 0;

    /** Currently active commands, empty when nothing executes */
    final private Deque<ScanCommandImpl<?>> active_commands = new ConcurrentLinkedDeque<>();

    /** {@link Future}, set when scan has been submitted to {@link ExecutorService}. Not reset back to empty. */
    private volatile Optional<Future<Object>> future = Optional.empty();

    /** Device Names for status PVs.
     *  They should either all be set or all be empty, so checking one is sufficient.
     */
    private Optional<String> device_active = Optional.empty(), device_status = Optional.empty(), device_state = Optional.empty(), device_progress = Optional.empty(), device_finish = Optional.empty();

    /** Timeout for updating the state PV
     *
     *  <p>Scan state PV is always updated awaiting completion,
     *  using this timeout.
     *
     *  <p>This allows additional database logic to for example
     *  latch scan failures, or to conditionally update a scan alarm PV.
     *  Scans can check the scan alarm PV in the pre-scan
     *  to prohibit further scans until alarm has been cleared.
     */
    final private static Duration timeout = Duration.ofSeconds(10);

    /** Initialize
     *  @param engine {@link ScanEngine} that executes this scan
     *  @param jython Jython support
     *  @param name User-provided name for this scan
     *  @param devices {@link DeviceContext} to use for scan
     *  @param pre_scan Commands to execute before the 'main' section of the scan
     *  @param implementations Commands to execute in this scan
     *  @param post_scan Commands to execute before the 'main' section of the scan
     *  @throws Exception on error (cannot access log, ...)
     */
    public ExecutableScan(final ScanEngine engine, final JythonSupport jython, final String name, final DeviceContext devices,
            final List<ScanCommandImpl<?>> pre_scan,
            final List<ScanCommandImpl<?>> implementations,
            final List<ScanCommandImpl<?>> post_scan) throws Exception
    {
        super(DataLogFactory.createDataLog(name));
        this.engine = engine;
        this.jython = jython;
        this.macros = new MacroContext(ScanServerInstance.getScanConfig().getMacros());
        this.devices = devices;
        this.pre_scan = pre_scan;
        this.implementations = implementations;
        this.post_scan = post_scan;

        // Assign addresses to all commands,
        // determine work units
        long address = 0;
        long work_units = 0;
        for (ScanCommandImpl<?> impl : implementations)
        {
            address = impl.setAddress(address);
            work_units += impl.getWorkUnits();
        }
        total_work_units = work_units;
    }

    /** @return {@link QueueState} */
    QueueState getQueueState()
    {
        return queue_state;
    }

    /** @param state {@link QueueState} */
    void setQueueState(final QueueState state)
    {
        queue_state = state;
    }


    /** Submit scan for execution
     *  @param executor {@link ExecutorService} to use
     *  @return Future to cancel or await completion
     *  @throws IllegalStateException if scan had been submitted before
     */
    public Future<Object> submit(final ExecutorService executor)
    {
        if (future.isPresent())
            throw new IllegalStateException("Already submitted for execution");
        final Future<Object> the_future = executor.submit(this);
        future = Optional.of(the_future);
        return the_future;
    }

    /** @return {@link ScanState} */
    @Override
    public ScanState getScanState()
    {
        return state.get();
    }

    /** @return Info about current state of this scan */
    @Override
    public ScanInfo getScanInfo()
    {
        final ScanCommandImpl<?> command = active_commands.peekLast();
        final long address = command == null ? -1 : command.getCommand().getAddress();
        final String command_name;
        final ScanState state = getScanState();
        final long runtime;
        final long performed_work_units;

        if (start_ms <= 0)
        {   // Not started
            command_name = "";
            runtime = 0;
            performed_work_units = 0;
        }
        else if (state.isDone())
        {   // Finished, aborted
            command_name = "- end -";
            runtime = end_ms - start_ms;
            performed_work_units = total_work_units;
        }
        else
        {   // Running
            command_name = command == null ? "" : command.toString();
            final long now = System.currentTimeMillis();
            runtime = now - start_ms;
            performed_work_units = work_performed.get();

            // Estimate end time
            final long finish_estimate = performed_work_units <= 0
                ? now
                : start_ms + runtime*total_work_units/performed_work_units;

            // Somewhat smoothly update end time w/ estimate
            if (end_ms <= 0)
                end_ms = finish_estimate;
            else
                end_ms = 4*(end_ms/5) + finish_estimate/5;
        }

        return new ScanInfo(this, state, error, runtime, end_ms, performed_work_units, total_work_units, address, command_name);
    }

    /** @return Commands executed by this scan */
    public List<ScanCommand> getScanCommands()
    {
        // Fetch underlying commands for implementations
        final List<ScanCommand> commands = new ArrayList<>(implementations.size());
        for (ScanCommandImpl<?> impl : implementations)
            commands.add(impl.getCommand());
        return commands;
    }

    /** @param address Command address
     *  @return ScanCommand with that address
     *  @throws Exception when not found
     */
    public ScanCommand getCommandByAddress(final long address) throws Exception
    {
        final ScanCommand found = findCommandByAddress(getScanCommands(), address);
        if (found == null)
            throw new Exception("Invalid command address " + address);
        return found;
    }

    /** Recursively search for command by address
     *  @param commands Command list
     *  @param address Desired command address
     *  @return Command with that address or <code>null</code>
     */
    private ScanCommand findCommandByAddress(final List<ScanCommand> commands,
            final long address)
    {
        for (ScanCommand command : commands)
        {
            if (command.getAddress() == address)
                return command;
            else if (command instanceof LoopCommand)
            {
                final LoopCommand loop = (LoopCommand) command;
                final ScanCommand found = findCommandByAddress(loop.getBody(), address);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /** Attempt to update a command parameter to a new value
     *  @param address Address of the command
     *  @param property_id Property to update
     *  @param value New value for the property
     *  @throws Exception on error
     */
    public void updateScanProperty(final long address, final String property_id,
        final Object value) throws Exception
    {
        final ScanCommand command = getCommandByAddress(address);
        logger.log(Level.WARNING, "Updating running scan, changing " + property_id + " to " + value + " in " + command);
        try
        {
            command.setProperty(property_id, value);
        }
        catch (Exception ex)
        {
            throw new Exception("Cannot update " + property_id + " of " +
                    command.getCommandName(), ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public MacroContext getMacros()
    {
        return macros;
    }

    /** Obtain devices used by this scan.
     *
     *  <p>Note that the result can differ before and
     *  after the scan gets executed because devices
     *  are added to the device context as needed.
     *
     *  @return Devices used by this scan
     */
    public Device[] getDevices()
    {
        return devices.getDevices();
    }

    /** {@inheritDoc} */
    @Override
    public Device getDevice(final String name) throws Exception
    {
        return devices.getDevice(name);
    }

    /** {@inheritDoc} */
    @Override
    public void setLogMode(final boolean automatic)
    {
        automatic_log_mode = automatic;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAutomaticLogMode()
    {
        return automatic_log_mode;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<DataLog> getDataLog()
    {
        return data_logger;
    }

    /** {@inheritDoc} */
    @Override
    public long getLastScanDataSerial() throws Exception
    {
        final DataLog logger = data_logger.orElse(null);
        if (logger == null)
            return super.getLastScanDataSerial();
        return logger.getLastScanDataSerial();
    }

    /** {@inheritDoc} */
    @Override
    public ScanData getScanData() throws Exception
    {
        final DataLog logger = data_logger.orElse(null);
        if (logger == null)
            return super.getScanData();
        return logger.getScanData();
    }

    /** Callable for executing all commands on the scan,
     *  turning exceptions into a 'Failed' scan state.
     */
    @Override
    public Object call() throws Exception
    {
        logger.log(Level.CONFIG, "Executing ID " + getId() + " \"" + getName() + "\" [" + new MemoryInfo() + "]");

        try
        {
            // Set logger for execution of scan
            try
            {
                data_logger = Optional.of(DataLogFactory.getDataLog(this));
            }
            catch (Exception dl_ex)
            {
                throw new Exception("Aborted while opening data log", dl_ex);
            }
            execute_or_die_trying();
            // Exceptions will already have been caught within execute_or_die_trying,
            // hopefully updating the status PVs, but there could be exceptions
            // when connecting or closing devices which we'll catch here
        }
        catch (InterruptedException ex)
        {
            state.set(ScanState.Aborted);
            error = Optional.of(ScanState.Aborted.name());
        }
        catch (Throwable ex)
        {
            error = Optional.of(ex.getMessage());
            // Scan may have been aborted early on, for example in DataLogFactory.getDataLog()
            // Otherwise consider it failed
            if (state.get() == ScanState.Aborted)
                logger.log(Level.WARNING, "Scan " + getName() + " aborted", ex);
            else
            {
                state.set(ScanState.Failed);
                logger.log(Level.WARNING, "Scan " + getName() + " failed", ex);
            }
        }
        // Set actual end time, not estimated
        end_ms = System.currentTimeMillis();
        // Close data logger
        if (data_logger.isPresent())
            data_logger.get().close();
        data_logger = Optional.empty();
        logger.log(Level.CONFIG, "Completed ID " + getId() + " \"" + getName() + "\"");

        return null;
    }

    /** Execute all commands on the scan,
     *  passing exceptions back up.
     *  @throws Exception on error
     */
    private void execute_or_die_trying() throws Exception
    {
        // Was scan aborted before it ever got to run?
        if (state.get() == ScanState.Aborted)
            return;
        // Otherwise expect 'Idle'
        if (! state.compareAndSet(ScanState.Idle, ScanState.Running))
            throw new IllegalStateException("Cannot run Scan that is " + state.get());

        start_ms = System.currentTimeMillis();

        // Locate devices for status PVs
        final String prefix = ScanServerInstance.getScanConfig().getStatusPvPrefix();
        if (prefix != null   &&   !prefix.isEmpty())
        {
            device_active = Optional.of(prefix + "Active");
            devices.addPVDevice(new DeviceInfo(device_active.get()));

            device_status = Optional.of(prefix + "Status");
            devices.addPVDevice(new DeviceInfo(device_status.get()));

            device_state = Optional.of(prefix + "State");
            devices.addPVDevice(new DeviceInfo(device_state.get()));

            device_progress = Optional.of(prefix + "Progress");
            devices.addPVDevice(new DeviceInfo(device_progress.get()));

            device_finish = Optional.of(prefix + "Finish");
            devices.addPVDevice(new DeviceInfo(device_finish.get()));
        }

        // Add devices used by commands
        DeviceContextHelper.addScanDevices(devices, macros, pre_scan);
        DeviceContextHelper.addScanDevices(devices, macros, implementations);
        DeviceContextHelper.addScanDevices(devices, macros, post_scan);

        // Start Devices
        devices.startDevices();

        // Execute commands
        try
        {
            // Start all devices, which includes the optional device_state etc.
            // This means device_state is not updated until all connect,
            // which is probably OK because not really "Running" until they do.
            execute(new WaitForDevicesCommandImpl(new WaitForDevicesCommand(devices.getDevices()), null));

            // Initialize scan status PVs. Error will prevent scan from starting.
            if (device_active.isPresent())
            {
                getDevice(device_status.get()).write(getName());
                ScanCommandUtil.write(this, device_state.get(), getScanState().ordinal());
                ScanCommandUtil.write(this, device_active.get(), Double.valueOf(1.0));
                ScanCommandUtil.write(this, device_progress.get(), Double.valueOf(0.0));
                getDevice(device_finish.get()).write("Starting ...");
                logger.log(Level.INFO, this + " sets "  + device_active.get() + " = 1.0");
            }

            try
            {
                // Execute pre-scan commands
                execute(pre_scan);

                // Reset work step counter to only count the 'main' commands
                work_performed.set(0);

                // Execute the submitted commands
                execute(implementations);

                // Successful finish
                state.set(ScanState.Finished);
            }
            finally
            {
                // Try post-scan commands even if submitted commands ran into problems or were aborted.
                // Save the state before going back to Running for post commands.
                final long saved_steps = work_performed.get();
                final ScanState saved_state = state.getAndSet(ScanState.Running);

                execute(post_scan);

                // Restore saved state
                work_performed.set(saved_steps);
                state.set(saved_state);
            }
        }
        catch (Exception ex)
        {
            if (state.get() == ScanState.Aborted)
                error = Optional.of(ScanState.Aborted.name());
            else
            {
                String message = ex.getMessage();
                if (message != null)
                {   // Remove initial "java..Exception: " because that tends
                    // to misguide many users in seeing a Java problem instead of
                    // reading the actual message.
                    // This tends to happen with nested messages which return
                    // the 'cause', starting with its class name.
                    message = java_exception_pattern.matcher(message).replaceFirst("");
                    error = Optional.of(message);
                }
                else
                    error = Optional.of(ex.getClass().getName());
                state.set(ScanState.Failed);
                logger.log(Level.WARNING, "Scan " + getName() + " failed", ex);
            }
        }
        finally
        {
            try
            {
                end_ms = System.currentTimeMillis();

                // Assert that the state is 'done'
                // to exclude this scan from the engine.hasPendingScans() information
                state.getAndUpdate(current ->
                {
                    if (current.isDone())
                        return current;
                    logger.log(Level.WARNING, "Scan state was %s, changing to Failed", current.toString());
                    return ScanState.Failed;
                });

                // Final status PV update.
                if (device_active.isPresent())
                {
                    getDevice(device_status.get()).write("");
                    ScanCommandUtil.write(this, device_state.get(), getScanState().ordinal(), true, true, device_state.get(), 0.1, timeout);
                    logger.log(Level.INFO, this + " sets "  + device_state.get() + " = " + getScanState());
                    getDevice(device_finish.get()).write(TimestampFormats.MILLI_FORMAT.format(Instant.now()));
                    ScanCommandUtil.write(this, device_progress.get(), Double.valueOf(100.0));
                    // Update to "anything else running?"
                    final int active = engine.hasPendingScans() ? 1 : 0;
                    ScanCommandUtil.write(this, device_active.get(), active);
                    logger.log(Level.INFO, this + " sets "  + device_active.get() + " = " + active);
                }
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, "Scan finalization failed", ex);
            }

            // Stop devices
            devices.stopDevices();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final List<ScanCommandImpl<?>> commands) throws Exception
    {
        for (ScanCommandImpl<?> command : commands)
        {
            final ScanState current_state = state.get();
            if (current_state != ScanState.Running  &&
                current_state != ScanState.Paused)
                return;
            execute(command);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanCommandImpl<?> command) throws Exception
    {
        active_commands.addLast(command);
        try
        {
            while (state.get() == ScanState.Paused)
            {   // Pause until resumed
                synchronized (this)
                {
                    wait();
                }
            }

            executeWithRetries(command);

            // Try to update Scan PVs on progress. Log errors, but continue scan
            if (device_progress.isPresent())
            {
                final ScanInfo info = getScanInfo();
                try
                {
                    ScanCommandUtil.write(this, device_progress.get(), Double.valueOf(info.getPercentage()));
                    getDevice(device_finish.get()).write(TimestampFormats.formatCompactDateTime(info.getFinishTime()));
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Error updating status PVs", ex);
                }
            }
        }
        finally
        {
            active_commands.remove(command);
        }
    }

    /** @param command Command to execute, allowing for error handling and retries
     *  @throws Exception on error
     */
    private void executeWithRetries(final ScanCommandImpl<?> command) throws Exception
    {
        while (true)
        {
            try
            {
                logger.log(Level.INFO, "@{0}: {1}", new Object[] { command.getCommand().getAddress(), command });
                command.execute(this);
                return; // Command executed without error
            }
            catch (Exception error)
            {   // Was command interrupted on purpose?
                // That would typically result in an 'InterruptedException',
                // but interrupted command might wrap that into a different type
                // of exception
                // -> Best way to detect an abort is via the scan state
                if (state.get() == ScanState.Aborted)
                {
                    final String message = "Command aborted: " + command.toString();
                    logger.log(Level.INFO, message, error);
                    throw error;
                }

                // Command generated an error
                final String message = "Command failed: " + command.toString();
                logger.log(Level.WARNING, message, error);
                // Error handler determines how to proceed
                switch (command.handleError(this, error))
                {
                case Abort:
                    // Abort on the original error
                    throw error;
                case Continue:
                    // Ignore the error, move on
                    return;
                case Retry:
                    // Stay in 'while' for a retry
                }
            }
        }
    }

    /** Force transition to next command */
    public void next()
    {
        // Must be running
        if (state.get() != ScanState.Running)
            return;
        // Must have active command
        final ScanCommandImpl<?> command = active_commands.peekLast();
        if (command == null)
            return;
        logger.log(Level.INFO, "Forcing transition to next command of " + this);
        command.next();
    }

    /** Pause execution of a currently executing scan */
    public void pause()
    {
        if (! state.compareAndSet(ScanState.Running, ScanState.Paused))
            return;
        logger.log(Level.INFO, "Pause " + this);

        if (device_state.isPresent())
        {
            try
            {
                ScanCommandUtil.write(this, device_state.get(), getScanState().ordinal(), true, true, device_state.get(), 0.1, timeout);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error updating state PV", ex);
            }
        }
    }

    /** Resume execution of a paused scan */
    public void resume()
    {
        if (! state.compareAndSet(ScanState.Paused, ScanState.Running))
            return;
        logger.log(Level.INFO, "Resume " + this);

        if (device_state.isPresent())
        {
            try
            {
                ScanCommandUtil.write(this, device_state.get(), getScanState().ordinal(), true, true, device_state.get(), 0.1, timeout);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error updating state PV", ex);
            }
        }

        synchronized (this)
        {   // Wake thread waiting for Paused state to end
            notifyAll();
        }
    }

    /** Mark for abort
     *  @return Previous scan state
     */
    public ScanState prepareAbort()
    {
        // Set state to aborted unless it is already 'done'
        return state.getAndUpdate((current_state)  ->  current_state.isDone() ? current_state : ScanState.Aborted);
    }

    /** Abort scan
     *  @param previous Previous state from `prepareAbort()`
     */
    public void doAbort(final ScanState previous)
    {
        if (previous.isDone())
            return;
        logger.log(Level.INFO, "Abort " + this + " (" + previous + ")");

        // Interrupt, except when already aborted, failed, ..
        // to prevent interruption when in the middle of updating scan state PVs
        final Future<Object> save = future.orElse(null);
        if (save != null  &&  ! save.isCancelled())
        {
            final boolean interrupt = previous == ScanState.Idle    ||
                                      previous == ScanState.Running ||
                                      previous == ScanState.Paused;
            save.cancel(interrupt);
            if (interrupt)
                logger.log(Level.INFO, "Interrupted " + this);
            else
                logger.log(Level.INFO, "Cancelled " + this);
        }
        synchronized (this)
        {
            notifyAll();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void workPerformed(final int work_units)
    {
        work_performed.addAndGet(work_units);
    }

    /** Release resources */
    @Override
    public void close() throws Exception
    {
        jython.close();
        pre_scan.clear();
        pre_scan.clear();
        implementations.clear();
        post_scan.clear();
        active_commands.clear();
    }
}
