package org.phoebus.applications.saveandrestore.ui;

public class SearchQueryUtil {

    // Ordered search keys
    public enum Keys {
        NAME("name"),
        DESC("desc"),
        USER("user"),
        TAGS("tags"),
        TYPE("type"),
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
