package org.phoebus.applications.alarm.client;

import org.phoebus.applications.alarm.Messages;

import java.util.Set;


public enum AlarmClientState {

    /**
     * Start state. There is no way to reach this from other states.
     *
     * CONNECTING -> NO_INITIAL_KAFKA_CONNECTION
     * CONNECTING -> LOADING
     */
    CONNECTING,

    /**
     * Indicates we were either unable to initially reach Kafka at all, or we were
     * disconnected from Kafka while loading the initial flurry of Kafka messages.
     *
     * This exists alongside NO_KAFKA_CONNECTION so we know to go back to LOADING
     * rather than CONNECTED.
     *
     * NO_INITIAL_KAFKA_CONNECTION -> LOADING
     */
    NO_INITIAL_KAFKA_CONNECTION,

    /**
     * Indicates we're still loading the initial flurry of Kafka messages
     *
     * LOADING -> NO_INITIAL_KAFKA_CONNECTION
     * LOADING -> CONNECTED
     */
    LOADING,

    /**
     * Indicates we have been disconnected from Kafka at some point after we
     * connected initially and loaded the initial flurry of Kafka messages
     *
     * NO_KAFKA_CONNECTION -> CONNECTED
     */
    NO_KAFKA_CONNECTION,

    /**
     * CONNECTED -> NO_KAFKA_CONNECTION
     * CONNECTED -> NO_ALARMSERVER_CONNECTION
     */
    CONNECTED,

    /**
     * NO_ALARMSERVER_CONNECTION -> NO_KAFKA_CONNECTION
     * NO_ALARMSERVER_CONNECTION -> CONNECTED
     */
    NO_ALARMSERVER_CONNECTION,

    /**
     * Indicates an error has occurred while processing messages
     *
     * When this state is reached, the client will have stopped
     * processing messages - it is a final state
     */
    CLIENT_ERROR,

    /**
     * Indicates the client has shut down
     *
     * When this state is reached, the client will have stopped
     * processing messages - it is a final state
     */
    CLIENT_SHUT_DOWN;

    private static final Set<AlarmClientState> INITIAL_STATES = Set.of(CONNECTING, NO_INITIAL_KAFKA_CONNECTION, LOADING);
    private static final Set<AlarmClientState> SERVER_DISCONNECTED_STATES = Set.of(NO_INITIAL_KAFKA_CONNECTION, NO_KAFKA_CONNECTION, NO_ALARMSERVER_CONNECTION);

    /**
     * @return true if this state occurs prior to the config being loaded
     */
    public boolean isInitialState() {
        return INITIAL_STATES.contains(this);
    }

    /**
     * @return true if this state indicates Kafka or the alarm server are not connected
     */
    public boolean isServerDisconnectedState() {
        return SERVER_DISCONNECTED_STATES.contains(this);
    }

    public String getDisplayName() {
        return switch (this) {
            case CONNECTING -> Messages.connecting;
            case NO_INITIAL_KAFKA_CONNECTION -> Messages.noInitialKafkaConnection;
            case LOADING -> Messages.loading;
            case NO_KAFKA_CONNECTION -> Messages.noKafkaConnection;
            case CONNECTED -> Messages.connected;
            case NO_ALARMSERVER_CONNECTION -> Messages.noAlarmserverConnection;
            case CLIENT_ERROR -> Messages.clientError;
            case CLIENT_SHUT_DOWN -> Messages.clientShutDown;
        };
    }
}
