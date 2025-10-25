/*******************************************************************************
 * Copyright (c) 2017-2025 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.jackie;

import com.aquenos.epics.jackie.client.ChannelAccessChannel;
import com.aquenos.epics.jackie.client.ChannelAccessClient;
import com.aquenos.epics.jackie.client.ChannelAccessMonitor;
import com.aquenos.epics.jackie.client.ChannelAccessMonitorListener;
import com.aquenos.epics.jackie.common.exception.ChannelAccessException;
import com.aquenos.epics.jackie.common.protocol.ChannelAccessEventMask;
import com.aquenos.epics.jackie.common.protocol.ChannelAccessStatus;
import com.aquenos.epics.jackie.common.value.ChannelAccessAlarmSeverity;
import com.aquenos.epics.jackie.common.value.ChannelAccessAlarmStatus;
import com.aquenos.epics.jackie.common.value.ChannelAccessControlsValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessGettableValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessString;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessValueFactory;
import com.aquenos.epics.jackie.common.value.ChannelAccessValueType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.jackie.util.SimpleJsonParser;
import org.phoebus.pv.jackie.util.ValueConverter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Process variable representing a Channel Access channel.
 */
public class JackiePV extends PV {

    private record ParsedChannelName(
            String ca_name,
            boolean treat_char_as_long_string,
            UsePutCallback use_put_callback) {
    }

    private enum UsePutCallback {
        NO,
        YES,
        AUTO,
    }

    private static final Pattern RECORD_FIELD_AS_LONG_STRING_PATTERN = Pattern
            .compile(".+\\.[A-Z][A-Z0-9]*\\$");

    private final String ca_name;

    private final ChannelAccessChannel channel;

    private ChannelAccessMonitor<? extends ChannelAccessControlsValue<?>> controls_monitor;

    private final ChannelAccessMonitorListener<ChannelAccessControlsValue<?>> controls_monitor_listener = new ChannelAccessMonitorListener<>() {
        @Override
        public void monitorError(
                ChannelAccessMonitor<? extends ChannelAccessControlsValue<?>> monitor,
                ChannelAccessStatus status, String message) {
            controlsMonitorException(monitor,
                    new ChannelAccessException(status, message));
        }

        @Override
        public void monitorEvent(
                ChannelAccessMonitor<? extends ChannelAccessControlsValue<?>> monitor,
                ChannelAccessControlsValue<?> value) {
            controlsMonitorEvent(monitor, value);
        }
    };

    private boolean controls_value_expected;

    private ChannelAccessControlsValue<?> last_controls_value;

    private ChannelAccessTimeValue<?> last_time_value;

    private final Object lock = new Object();

    private final JackiePreferences preferences;

    private ChannelAccessMonitor<? extends ChannelAccessGettableValue<?>> time_monitor;

    private final ChannelAccessMonitorListener<ChannelAccessGettableValue<?>> time_monitor_listener = new ChannelAccessMonitorListener<>() {
        @Override
        public void monitorError(
                ChannelAccessMonitor<? extends ChannelAccessGettableValue<?>> monitor,
                ChannelAccessStatus status, String message) {
            timeMonitorException(monitor,
                    new ChannelAccessException(status, message));
        }

        @Override
        public void monitorEvent(
                ChannelAccessMonitor<? extends ChannelAccessGettableValue<?>> monitor,
                ChannelAccessGettableValue<?> value) {
            if (value.getType().isTimeType()) {
                timeMonitorEvent(monitor, (ChannelAccessTimeValue<?>) value);
            } else if (value.getType() == ChannelAccessValueType.DBR_STRING) {
                // We might receive a DBR_STRING if this channel uses the
                // special RTYP handling. In this case, we use the local time
                // and assume that there is no alarm. As an alternative, we
                // could create a value without an alarm status and time stamp,
                // but some application code might expect that there is always
                // this meta-data, so we rather generate it here.
                var string_value = (ChannelAccessString) value;
                var now = System.currentTimeMillis();
                var time_string = ChannelAccessValueFactory
                        .createTimeString(string_value.getValue(),
                                channel.getClient().getConfiguration().getCharset(),
                                ChannelAccessAlarmSeverity.NO_ALARM,
                                ChannelAccessAlarmStatus.NO_ALARM,
                                (int) (now / 1000L
                                        - ValueConverter.OFFSET_EPICS_TO_UNIX_EPOCH_SECONDS),
                                (int) (now % 1000L * 1000000L));
                timeMonitorEvent(monitor, time_string);
            } else {
                timeMonitorException(monitor, new RuntimeException(
                        "Received a monitor event with an value of the "
                                + "unexpected type "
                                + value.getType().name()
                                + "."));
            }
        }
    };

    private final boolean treat_char_as_long_string;

    private final UsePutCallback use_put_callback;

    /**
     * Create a PV backed by a Channel Access channel.
     * <p>
     * Typically, this constructor should not be used directly. Instances
     * should be received from {@link JackiePVFactory} through the
     * {@link org.phoebus.pv.PVPool} instead.
     *
     * @param client      CA client that is used for connecting the PV to the
     *                    CA channel.
     * @param preferences preferences for the Jackie client. This should be the
     *                    same preferences that were also used when creating
     *                    the <code>client</code>.
     * @param name        name of the PV (possibly including a prefix).
     * @param base_name   name of the PV without the prefix.
     */
    public JackiePV(
            ChannelAccessClient client,
            JackiePreferences preferences,
            String name,
            String base_name) {
        super(name);
        logger.fine(getName() + " creating EPICS Jackie PV.");
        var parse_name_result = parseName(base_name);
        this.ca_name = parse_name_result.ca_name;
        this.treat_char_as_long_string = parse_name_result.treat_char_as_long_string;
        this.use_put_callback = parse_name_result.use_put_callback;
        this.preferences = preferences;
        // The PV base class starts of in a read-write state. We cannot know
        // whether the PV is actually writable before the connection has been
        // established, so we rather start in the read-only state.
        this.notifyListenersOfPermissions(true);
        this.channel = client.getChannel(this.ca_name);
        this.channel.addConnectionListener(this::connectionEvent);
    }

    @Override
    public CompletableFuture<VType> asyncRead() throws Exception {
        final var force_array = channel.getNativeCount() != 1;
        final var listenable_future = channel.get(
                timeTypeForNativeType(channel.getNativeDataType()));
        logger.fine(getName() + " reading asynchronously.");
        final var completable_future = new CompletableFuture<VType>();
        listenable_future.addCompletionListener((future) -> {
            final ChannelAccessTimeValue<?> value;
            try {
                // We know that we requested a time value, so we can be sure
                // that we get one and can cast without further checks.
                value = (ChannelAccessTimeValue<?>) future.get();
                logger.fine(
                        getName()
                                + " asynchronous read completed successfully: "
                                + value);
            } catch (InterruptedException e) {
                // The listener is only called when the future has completed,
                // so we should never receive such an exception.
                Thread.currentThread().interrupt();
                completable_future.completeExceptionally(
                        new RuntimeException(
                                "Unexpected InterruptedException", e));
                return;
            } catch (ExecutionException e) {
                logger.log(
                        Level.FINE,
                        getName()
                                + " asynchronous read failed: "
                                + e.getMessage(),
                        e.getCause());
                completable_future.completeExceptionally(e.getCause());
                return;
            }
                ChannelAccessControlsValue<?> controls_value;
                final boolean controls_value_expected;
                synchronized (lock) {
                    controls_value = this.last_controls_value;
                    controls_value_expected = this.controls_value_expected;
                    // We only save the value that we received if it matches
                    // the type of the stored controls value of if we did not
                    // receive a control value yet. Conversely, we do not use
                    // the controls value if its type does not match.
                    if (controls_value == null
                            || controls_value.getType().toSimpleType().equals(
                                    value.getType().toSimpleType())) {
                        this.last_time_value = value;
                    } else {
                        controls_value = null;
                    }
                }
            // We do the conversion in a try-catch block because we have to
            // ensure that the future always completes (otherwise, a thread
            // waiting for it might be blocked indefinitely).
            final VType vtype;
            try {
                vtype = ValueConverter.channelAccessToVType(
                        controls_value,
                        value,
                        channel.getClient().getConfiguration().getCharset(),
                        force_array,
                        preferences.honor_zero_precision(),
                        treat_char_as_long_string);
                completable_future.complete(vtype);
            } catch (Throwable e) {
                completable_future.completeExceptionally(e);
                return;
            }
            // The description in the API documentation states that the
            // listeners are notified when a value is received through the use
            // of asyncRead(). However, if we have not received a controls
            // value yet, we cannot construct a VType with meta-data. In this
            // case, we do not notify the listeners now. They are notified when
            // we receive the controls value.
            if (!controls_value_expected || controls_value != null) {
                notifyListenersOfValue(vtype);
            }
        });
        return completable_future;
    }

    @Override
    public CompletableFuture<?> asyncWrite(Object new_value) throws Exception {
        return switch (use_put_callback) {
            case AUTO, YES -> {
                // Use ca_put_callback.
                final var listenable_future = channel.put(
                        ValueConverter.objectToChannelAccessSimpleOnlyValue(
                                channel.getName(),
                                new_value,
                                channel.getClient().getConfiguration()
                                        .getCharset(),
                                treat_char_as_long_string,
                                preferences.long_conversion_mode()));
                final var completable_future = new CompletableFuture<Void>();
                listenable_future.addCompletionListener((future) -> {
                    try {
                        future.get();
                        completable_future.complete(null);
                    } catch (InterruptedException e) {
                        // The listener is only called when the future has
                        // completed, so we should never receive such an
                        // exception.
                        Thread.currentThread().interrupt();
                        completable_future.completeExceptionally(
                                new RuntimeException(
                                        "Unexpected InterruptedException", e));
                    } catch (ExecutionException e) {
                        completable_future.completeExceptionally(e.getCause());
                    }
                });
                yield completable_future;
            }
            case NO -> {
                // Do not wait for the write operation to complete and instead
                // report completion immediately. This allows code that does
                // not have direct access to the API to avoid the use of
                // ca_put_callback, which can have side effects on the server.
                write(new_value);
                var future = new CompletableFuture<Void>();
                future.complete(null);
                yield future;
            }
        };
    }

    @Override
    public void write(Object new_value) throws Exception {
        switch (use_put_callback) {
            case AUTO, NO -> {
                // Use ca_put without a callback.
                channel.putNoCallback(
                        ValueConverter.objectToChannelAccessSimpleOnlyValue(
                                channel.getName(),
                                new_value,
                                channel.getClient().getConfiguration()
                                        .getCharset(),
                                treat_char_as_long_string,
                                preferences.long_conversion_mode()));
            }
            case YES -> {
                // Wait for the write operation to complete. This allows code
                // (e.g. OPIs) that does not have direct access to the API to
                // wait for the write operation to complete.
                var future = asyncWrite(new_value);
                try {
                    future.get();
                } catch (ExecutionException e) {
                    var cause = e.getCause();
                    try {
                        throw cause;
                    } catch (Error | Exception nested_e) {
                        throw nested_e;
                    } catch (Throwable nested_e) {
                        throw ExceptionUtils.asRuntimeException(nested_e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    protected void close() {
        logger.fine(getName() + " closing PV.");
        super.close();
        channel.destroy();
        // Destroying the channel implicitly destroys the monitors associated
        // with it, so we can simply set them to null.
        synchronized (lock) {
            controls_monitor = null;
            time_monitor = null;
        }
    }

    private void connectionEvent(ChannelAccessChannel channel, boolean now_connected) {
        if (now_connected) {
            logger.fine(getName() + " connected.");
            // Let the listeners now whether the channel is writable.
            boolean may_write;
            // This event handler is called in the same thread that changes the
            // connection state, so the channel cannot get disconnected while
            // we are inside the handler. However, it can be destroyed
            // asynchronously. Therefore, we simply return when we encounter an
            // IllegalStateException while calling one of the methods that only
            // work for connected channels.
            ChannelAccessValueType native_data_type;
            try {
                may_write = channel.isMayWrite();
                native_data_type = channel.getNativeDataType();
            } catch (IllegalStateException e) {
                return;
            }
            this.notifyListenersOfPermissions(!may_write);
            var controls_type = controlsTypeForNativeType(native_data_type);
            var time_type = timeTypeForNativeType(native_data_type);
            if (time_type == null) {
                // If we cannot convert the native type to a time type, we
                // cannot meaningfully register a monitor, so we keep the PV
                // disconnected.
                return;
            }
            // We have to set the controls_value_expected flag before
            // registering the monitor for time values. Otherwise, we might use
            // a wrong value when receiving the first time-value event.
            var controls_value_expected = (controls_type != null);
            // We always create the monitors, even if the channel is not
            // readable. In this case, the monitors will trigger an error which
            // will be passed on to code trying to read this PV.
            ChannelAccessMonitor<? extends ChannelAccessControlsValue<?>> controls_monitor = null;
            ChannelAccessMonitor<?> time_monitor;
            try {
                time_monitor = channel.monitor(
                        time_type, preferences.monitor_mask());
            } catch (IllegalStateException e) {
                return;
            }
            time_monitor.addMonitorListener(time_monitor_listener);
            if (controls_type != null) {
                if (preferences.dbe_property_supported()) {
                    try {
                        controls_monitor = createControlsMonitor(
                                channel, controls_type);
                    } catch (IllegalStateException e) {
                        time_monitor.destroy();
                        return;
                    }
                    controls_monitor.addMonitorListener(controls_monitor_listener);
                } else {
                    try {
                        channel.get(controls_type, 1)
                                .addCompletionListener((future) -> {
                                    ChannelAccessGettableValue<?> value;
                                    try {
                                        value = future.get();
                                    } catch (ExecutionException e) {
                                        if (e.getCause() != null) {
                                            controlsGetException(e.getCause());
                                        } else {
                                            controlsGetException(e);
                                        }
                                        return;
                                    } catch (Throwable e) {
                                        controlsGetException(e);
                                        return;
                                    }
                                    // We know that we requested a DBR_CTRL_*
                                    // value, so we can safely cast here.
                                    controlsGetSuccess(
                                            (ChannelAccessControlsValue<?>) value);
                                });
                    } catch (Throwable e) {
                        controlsGetException(e);
                    }
                }
            }
            synchronized (lock) {
                this.controls_value_expected = controls_value_expected;
                this.controls_monitor = controls_monitor;
                this.time_monitor = time_monitor;
            }
        } else {
            logger.fine(getName() + " disconnected.");
            // When the PV is closed asynchronously while we are in this event
            // handler, the references to the monitors might suddenly become
            // null, so we have to handle this situation.
            ChannelAccessMonitor<?> controls_monitor;
            ChannelAccessMonitor<?> time_monitor;
            synchronized (lock) {
                controls_monitor = this.controls_monitor;
                time_monitor = this.time_monitor;
                this.controls_monitor = null;
                this.time_monitor = null;
                // Delete last values, so that we do not accidentally use them
                // in event notifications when the channel gets connected
                // again.
                this.last_controls_value = null;
                this.last_time_value = null;
            }
            if (time_monitor != null) {
                time_monitor.destroy();
            }
            if (controls_monitor != null) {
                controls_monitor.destroy();
            }
            // Let the listeners now that the PV is no longer connected.
            this.notifyListenersOfDisconnect();
            // As the channel is disconnected now, we consider it to not be
            // writable.
            this.notifyListenersOfPermissions(true);
        }
    }

    private void controlsGetException(Throwable e) {
        // This method is only called if the controls_monitor is null, so we
        // can simply pass null to controlsMonitorEvent.
        controlsMonitorException(null, e);
    }

    private void controlsGetSuccess(ChannelAccessControlsValue<?> value) {
        // This method is only called if the controlsMonitor is null, so we can
        // simply pass null to controlsMonitorEvent.
        controlsMonitorEvent(null, value);
    }

    private void controlsMonitorEvent(
            ChannelAccessMonitor<? extends ChannelAccessControlsValue<?>> monitor_from_listener,
            ChannelAccessControlsValue<?> controls_value) {
        logger.fine(getName() + " received controls value: " + controls_value);
        // If the monitor instance passed to the listener is not the same
        // instance that we have here, we ignore the event. This can happen if
        // a late notification arrives after destroying the monitor. In this
        // case, controls_monitor is going to be null or a new monitor instance
        // while monitor_from_listener is going to be an old monitor instance.
        ChannelAccessTimeValue<?> time_value;
        synchronized (lock) {
            if (controls_monitor != monitor_from_listener) {
                return;
            }
            last_controls_value = controls_value;
            time_value = last_time_value;
        }
        // If we previously received a time value, we can notify the listeners
        // now. We do this without holding the lock in order to avoid potential
        // deadlocks. There is a very small chance that due to not holding the
        // lock, we might send an old value, but this should only happen when
        // the channel has been disconnected to being destroyed, and in this
        // case it should not matter any longer.
        if (time_value != null) {
            notifyListenersOfValue(controls_value, time_value);
        }
    }

    private void controlsMonitorException(
            ChannelAccessMonitor<? extends ChannelAccessControlsValue<?>> monitor_from_listener,
            Throwable e) {
        // If the monitor instance passed to the listener is not the same
        // instance that we have here, we ignore the event. This can happen if
        // a late notification arrives after destroying the monitor. In this
        // case, controls_monitor is going to be null or a new monitor instance
        // while monitor_from_listener is going to be an old monitor instance.
        synchronized (lock) {
            if (controls_monitor != monitor_from_listener) {
                return;
            }
        }
        logger.log(
                Level.WARNING,
                getName() + " monitor for DBR_CTRL_* value raised an exception.",
                e);
    }

    private ChannelAccessValueType controlsTypeForNativeType(
            ChannelAccessValueType native_data_type) {
        // Strings do not have additional meta-data, so registering a controls
        // monitor does not make sense.
        // If this channel is configured for long-string mode and we have a
        // DBR_CHAR, there is no sense in requesting the meta-data either
        // because we are not going to use it anyway.
        if (native_data_type == ChannelAccessValueType.DBR_STRING) {
            return null;
        } else if (treat_char_as_long_string
                && native_data_type == ChannelAccessValueType.DBR_CHAR) {
            return null;
        } else {
            return native_data_type.toControlsType();
        }
    }

    @SuppressWarnings("unchecked")
    private ChannelAccessMonitor<? extends ChannelAccessControlsValue<?>> createControlsMonitor(
            ChannelAccessChannel channel,
            ChannelAccessValueType controls_type
    ) {
        // We do not use the value received via this monitor, so requesting
        // more than a single element would be a waste of bandwidth.
        // We always request a DBR_CTRL_* type, so we can safely cast the
        // monitor.
        return (ChannelAccessMonitor<? extends ChannelAccessControlsValue<?>>) channel
                .monitor(controls_type, 1, ChannelAccessEventMask.DBE_PROPERTY);
    }

    private ParsedChannelName parseName(String pv_name) {
        // A PV name might consist of the actual CA channel name followed by
        // optional parameters that configure the behavior of this PV source.
        // In order to be compatible with the format used by the older DIIRT
        // integration of EPICS Jackie, we use the same format.
        // This means that these options are enclosed in curly braces and
        // follow a JSON-style syntax. We also use the same option names.
        // Extracting the JSON-string is a bit tricky: A valid channel name
        // might contain a space and curly braces, so we cannot simply cut at
        // the first combination of space and opening curly brace. JSON, on the
        // other hand, might contain objects within the object, so cutting at
        // the last combination of a space and opening curly brace is not
        // necessarily correct either.
        // However, channel names rarely contain spaces, so cutting at the
        // first occurrence of a space and an opening curly brace is a pretty
        // good assumption. If this does not work (the resulting string is not
        // valid JSON), we simply look for other places where we can cut.
        // If the string does not end with a closing curly brace, our life is
        // much simpler, and we can simply assume that there is no JSON string
        // at the end of the channel name.
        pv_name = pv_name.trim();
        String ca_name = null;
        var force_no_long_string = false;
        var use_put_callback = UsePutCallback.AUTO;
        var treat_char_as_long_string = false;
        if (pv_name.endsWith("}")) {
            var space_index = pv_name.indexOf(" {");
            Object json_obj = null;
            // We remember the first exception because the first place where we
            // cut the string is most likely the right place.
            IllegalArgumentException first_exception = null;
            while (space_index != -1) {
                try {
                    json_obj = SimpleJsonParser.parse(pv_name.substring(
                            space_index + 1));
                    first_exception = null;
                    break;
                } catch (IllegalArgumentException e) {
                    // We try a larger portion of the string, but we save the
                    // exception in case the other attempts fail as well.
                    if (first_exception == null) {
                        first_exception = e;
                    }
                }
                space_index = pv_name.indexOf(" {", space_index + 2);
            }
            if (first_exception != null) {
                logger.warning(
                        getName()
                                + " Ignoring JSON options in PV name because "
                                + "they cannot be parsed: "
                                + first_exception.getMessage());
            } else if (json_obj != null) {
                // json_obj must be a map because we know that the string
                // represents a JSON object (because of the curly braces).
                @SuppressWarnings("unchecked")
                var options = (Map<String, Object>) json_obj;
                var long_string_option = options.get("longString");
                if (Boolean.TRUE.equals(long_string_option)) {
                    treat_char_as_long_string = true;
                } else if (Boolean.FALSE.equals(long_string_option)) {
                    force_no_long_string = true;
                } else if (options.containsKey("longString")) {
                    logger.warning(
                            getName()
                                    + " illegal value for \"longString\" "
                                    + "option (true or false was expected). "
                                    + "Option is going to be ignored.");
                }
                var put_callback_option = options.get("putCallback");
                if (Boolean.TRUE.equals(put_callback_option)) {
                    use_put_callback = UsePutCallback.YES;
                } else if (Boolean.FALSE.equals(put_callback_option)) {
                    use_put_callback = UsePutCallback.NO;
                } else if (options.containsKey("putCallback")) {
                    logger.warning(
                            getName()
                                    + " illegal value for \"putCallback\" "
                                    + "option (true or false was expected). "
                                    + "Option is going to be ignored.");
                }
                ca_name = pv_name.substring(0, space_index);
            }
        }
        // If the ca_name has not been set yet, there is no valid JSON options
        // part and the full channel name is the actual channel name.
        if (ca_name == null) {
            ca_name = pv_name;
        }
        // When reading fields from an IOC's record, one can read them as long
        // strings (arrays of chars) by appending a dollar sign to the end of
        // their names. If we find a channel name that matches this scheme, we
        // assume that the array of chars should actually be treated as a
        // string.
        // We do not automatically set the treat_char_as_long_string option if
        // it has been explicitly set to false by the user.
        if (!treat_char_as_long_string && !force_no_long_string
                && RECORD_FIELD_AS_LONG_STRING_PATTERN
                .matcher(ca_name).matches()) {
            treat_char_as_long_string = true;
        }
        return new ParsedChannelName(
                ca_name, treat_char_as_long_string, use_put_callback);
    }

    private void notifyListenersOfValue(
            ChannelAccessControlsValue<?> controls_value,
            ChannelAccessTimeValue<?> time_value) {
        boolean force_array;
        try {
            force_array = channel.getNativeCount() != 1;
        } catch (IllegalStateException e) {
            // If the channel has been disconnected in the meantime, we skip
            // the notification.
            return;
        }
        var vtype = ValueConverter.channelAccessToVType(
                controls_value,
                time_value,
                channel.getClient().getConfiguration().getCharset(),
                force_array,
                preferences.honor_zero_precision(),
                treat_char_as_long_string);
        notifyListenersOfValue(vtype);
    }

    private void timeMonitorEvent(
            ChannelAccessMonitor<? extends ChannelAccessGettableValue<?>> monitor_from_listener,
            ChannelAccessTimeValue<?> time_value) {
        logger.fine(getName() + " received time value: " + time_value);
        // If the monitor instance passed to the listener is not the same
        // instance that we have here, we ignore the event. This can happen if
        // a late notification arrives after destroying the monitor. In this
        // case, time_monitor is going to be null or a new monitor instance
        // while monitor_from_listener is going to be an old monitor instance.
        ChannelAccessControlsValue<?> controls_value;
        synchronized (lock) {
            if (time_monitor != monitor_from_listener) {
                return;
            }
            last_time_value = time_value;
            controls_value = last_controls_value;
        }
        // If we previously received a time value, we can notify the listeners
        // now. We do this without holding the lock in order to avoid potential
        // deadlocks. There is a very small chance that due to not holding the
        // lock, we might send an old value, but this should only happen when
        // the channel has been disconnected to being destroyed, and in this
        // case it should not matter any longer.
        if (controls_value != null || !controls_value_expected) {
            notifyListenersOfValue(controls_value, time_value);
        }
    }

    private void timeMonitorException(
            ChannelAccessMonitor<? extends ChannelAccessGettableValue<?>> monitor_from_listener,
            Throwable e) {
        // If the monitor instance passed to the listener is not the same
        // instance that we have here, we ignore the event. This can happen if
        // a late notification arrives after destroying the monitor. In this
        // case, time_monitor is going to be null or a new monitor instance
        // while monitor_from_listener is going to be an old monitor instance.
        synchronized (lock) {
            if (time_monitor != monitor_from_listener) {
                return;
            }
        }
        logger.log(
                Level.WARNING,
                getName() + " monitor for DBR_TIME_* value raised an exception.",
                e);
    }

    private ChannelAccessValueType timeTypeForNativeType(
            ChannelAccessValueType native_data_type) {
        // If the corresponding configuration flag is enabled, we want to handle
        // the RTYP field in a special way.
        if (preferences.rtyp_value_only()
                && native_data_type == ChannelAccessValueType.DBR_STRING
                && ca_name.endsWith(".RTYP")) {
            return native_data_type;
        }
        // In theory, it is possible that the server sends a data-type that has
        // no corresponding DBR_TIME_* type. In particular, this happens if it
        // sends a DBR_PUT_ACKT, DBR_PUT_ACKS, DBR_STSACK_STRING, or
        // DBR_CLASS_NAME. Sending a DBR_PUT_ACKT or DBR_PUT_ACKS are only used
        // in write operations and DBR_STSACK_STRING and DBR_CLASS_NAME are
        // only used in read operations when specifically requested. In fact,
        // the CA server of EPICS Base will never report such a native type and
        // as it does not make much sense, it is unlikely any other
        // implementation will. Thus, we log an error and simply keep the
        // channel disconnected.
        try {
            return native_data_type.toTimeType();
        } catch (IllegalArgumentException e) {
            logger.severe(
                    getName()
                            + " server returned unexpected native type: "
                            + native_data_type.name());
            return null;
        }
    }

}
