package org.phoebus.core.types;

import java.time.Instant;

@SuppressWarnings("nls")
public class TimeStampedProcessVariable extends ProcessVariable {

    private final Instant time;

    public TimeStampedProcessVariable(String name, Instant instant) {
        super(name);
        this.time = instant;
    }

    public Instant getTime() {
        return time;
    }


    @Override
    public String toString() {
        return "TimeStampedProcessVariable(" + getName() + ", " + time + ")";
    }
}
