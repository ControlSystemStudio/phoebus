package org.phoebus.applications.alarm;

import java.util.logging.Logger;

public class AlarmSystemConstants {
    /** Alarm system logger */
    public static final Logger logger = Logger.getLogger(AlarmSystem.class.getPackageName());

    /** Path prefix for config updates */
    public static final String CONFIG_PREFIX = "config:";

    /** Path prefix for state updates */
    public static final String STATE_PREFIX = "state:";

    /** Path prefix for commands */
    public static final String COMMAND_PREFIX = "command:";

    /** Path prefix for talk messages */
    public static final String TALK_PREFIX = "talk:";

    // In principle, all messages can be sent via the same topic,
    // which also asserts that their order is preserved.
    // The command and talk topics are sent via separate topics
    // because they are one-directional and Kafka can be configured
    // to delete older talk and command messages,
    // while state and config need to be compacted (or kept forever).

    /** Suffix for the topic that clients use to send commands to alarm server */
    public static final String COMMAND_TOPIC_SUFFIX = "Command";

    /** Suffix for the topic that server uses to send annunciations */
    public static final String TALK_TOPIC_SUFFIX = "Talk";

    /** Suffix for the topic that contains non compacted aggregate of other topics. */
    public static final String LONG_TERM_TOPIC_SUFFIX = "LongTerm";
}
