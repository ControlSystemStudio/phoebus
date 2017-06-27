package org.phoebus.logging;

import java.time.Instant;

/**
 * A factory for creating LogEntry
 * @author Kunal Shroff
 *
 */
public class LogEntryFactory {

    private String text;
    private String owner;
    private final Instant time = Instant.now();
    private final Instant modifyTime = Instant.now();

    private LogEntryFactory(String text) {
        this.text = text;
    }

    public static LogEntryFactory buildLogEntry() {
        return new LogEntryFactory("");
    }

    public static LogEntryFactory buildLogEntry(String text) {
        return new LogEntryFactory(text);
    }

    public LogEntryFactory withOwner(String owner) {
        this.owner = owner;
        return this;
    }
    
    public LogEntry create() {
        return new LogEntryImpl(this.text, this.owner, this.time, this.modifyTime);
    }
    
    class LogEntryImpl implements LogEntry {
        private final String text;
        private final String owner;
        private final Instant time;
        private final Instant modifyTime;

        public LogEntryImpl(String text, String owner, Instant time, Instant modifyTime) {
            super();
            this.text = text;
            this.owner = owner;
            this.time = time;
            this.modifyTime = modifyTime;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public String getOwner() {
            return owner;
        }

        @Override
        public Instant getTime() {
            return time;
        }

        @Override
        public Instant getModifyTime() {
            return modifyTime;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Create Time: " + getTime() + " Created by: " + getOwner());
            sb.append(System.lineSeparator());
            sb.append(getText());
            return sb.toString();
        }
    }
}
