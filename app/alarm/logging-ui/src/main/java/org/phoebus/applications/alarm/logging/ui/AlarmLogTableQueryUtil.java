package org.phoebus.applications.alarm.logging.ui;

public class AlarmLogTableQueryUtil {

    // Ordered search keys
    public enum Keys {
        PV("pv"),
        ROOT("root"),
        SEVERITY("severity"),
        MESSAGE("message"),
        CURRENTSEVERITY("current_severity"),
        CURRENTMESSAGE("current_message"),
        USER("user"),
        HOST("host"),
        COMMAND("command"),
        STARTTIME("start"),
        ENDTIME("end");

        private final String name;

        Keys(String name) {
            this.name = name;
        };

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
