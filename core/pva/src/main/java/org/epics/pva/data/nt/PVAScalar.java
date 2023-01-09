package org.epics.pva.data.nt;

import java.util.Arrays;
import java.util.Objects;

import org.epics.pva.data.PVABool;
import org.epics.pva.data.PVABoolArray;
import org.epics.pva.data.PVAByte;
import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAFloat;
import org.epics.pva.data.PVAFloatArray;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVALong;
import org.epics.pva.data.PVALongArray;
import org.epics.pva.data.PVAShort;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;

/**
 * Normative scalar and scaler[] type
 * 
 * NTScalar :=
 * <ul>
 * <li>structure
 * <ul>
 * <li>scalar_t value or scalar_t[] value
 * <li>string descriptor :opt
 * <li>alarm_t alarm :opt
 * <li>time_t timeStamp :opt
 * <li>display_t display :opt
 * <li>control_t control :opt
 * 
 * where scalar_t can be:
 * <ul>
 * <li>{@link PVABool}
 * <li>{@link PVAByte}
 * <li>{@link PVAShort}
 * <li>{@link PVAInt}
 * <li>{@link PVALong}
 * <li>{@link PVAFloat}
 * <li>{@link PVADouble}
 * <li>{@link PVAString}
 * 
 * and scalar_t[] can be:
 * <ul>
 * <li>{@link PVABoolArray}
 * <li>{@link PVAByteArray}
 * <li>{@link PVAShortArray}
 * <li>{@link PVAIntArray}
 * <li>{@link PVALongArray}
 * <li>{@link PVAFloatArray}
 * <li>{@link PVADoubleArray}
 * <li>{@link PVAStringArray}
 */
public class PVAScalar<S extends PVAData> extends PVAStructure {
    public static final String STRUCT_NAME_STRING = "epics:nt/NTScalar:1.0";
    public static final String VALUE_NAME_STRING = "value";
    public static final String DESCRIPTION_NAME_STRING = "description";

    public static class Builder<S extends PVAData> {

        private String name;
        private S value;
        private PVAString description;
        private PVAAlarm alarm;
        private PVATimeStamp timeStamp;
        private PVADisplay display;
        private PVAControl control;

        public Builder() {
        }

        Builder(String name, S value, PVAString description, PVAAlarm alarm, PVATimeStamp timeStamp, PVADisplay display,
                PVAControl control) {
            this.name = name;
            this.value = value;
            this.description = description;
            this.alarm = alarm;
            this.timeStamp = timeStamp;
            this.display = display;
            this.control = control;
        }

        public Builder<S> name(String name) {
            this.name = name;
            return Builder.this;
        }

        public Builder<S> value(S value) {
            this.value = value;
            return Builder.this;
        }

        public Builder<S> description(PVAString description) {
            this.description = description;
            return Builder.this;
        }

        public Builder<S> alarm(PVAAlarm alarm) {
            this.alarm = alarm;
            return Builder.this;
        }

        public Builder<S> timeStamp(PVATimeStamp timeStamp) {
            this.timeStamp = timeStamp;
            return Builder.this;
        }

        public Builder<S> display(PVADisplay display) {
            this.display = display;
            return Builder.this;
        }

        public Builder<S> control(PVAControl control) {
            this.control = control;
            return Builder.this;
        }

        public PVAScalar<S> build() throws PVAScalarValueNameException, PVAScalarDescriptionNameException {
            if (this.name == null) {
                throw new NullPointerException("The property \"name\" is null. "
                        + "Please set the value by \"name()\". "
                        + "The properties \"name\", \"value\" are required.");
            }
            if (this.value == null) {
                throw new NullPointerException("The property \"value\" is null. "
                        + "Please set the value by \"value()\". "
                        + "The properties \"name\", \"value\" are required.");
            }
            if (!value.getName().equals(VALUE_NAME_STRING)) {
                throw new PVAScalarValueNameException(value.getName());
            }
            if (this.description != null && !description.getName().equals(DESCRIPTION_NAME_STRING)) {
                throw new PVAScalarDescriptionNameException(description.getName());
            }
            return new PVAScalar<>(this);
        }
    }

    private PVAScalar(Builder<S> builder) {
        super(builder.name, STRUCT_NAME_STRING,
                Arrays.stream(new PVAData[] { builder.value, builder.alarm, builder.control, builder.description,
                        builder.display, builder.timeStamp }).filter(Objects::nonNull).toArray(PVAData[]::new));
    }

    public static Builder<PVABool> boolScalarBuilder(boolean value) {
        return new Builder<PVABool>().value(new PVABool(VALUE_NAME_STRING, value));
    }

    public static Builder<PVAByte> byteScalarBuilder(boolean unsigned, byte value) {
        return new Builder<PVAByte>().value(new PVAByte(VALUE_NAME_STRING, unsigned, value));
    }

    public static Builder<PVAShort> shortScalarBuilder(final boolean unsigned, final short value) {
        return new Builder<PVAShort>().value(new PVAShort(VALUE_NAME_STRING, unsigned, value));
    }

    public static Builder<PVAInt> intScalarBuilder(int value) {
        return new Builder<PVAInt>().value(new PVAInt(VALUE_NAME_STRING, value));
    }

    public static Builder<PVALong> longScalarBuilder(final boolean unsigned, final long value) {
        return new Builder<PVALong>().value(new PVALong(VALUE_NAME_STRING, unsigned, value));
    }

    public static Builder<PVAFloat> floatScalarBuilder(float value) {
        return new Builder<PVAFloat>().value(new PVAFloat(VALUE_NAME_STRING, value));
    }

    public static Builder<PVADouble> doubleScalarBuilder(double value) {
        return new Builder<PVADouble>().value(new PVADouble(VALUE_NAME_STRING, value));
    }

    public static Builder<PVAString> stringScalarBuilder(String value) {
        return new Builder<PVAString>().value(new PVAString(VALUE_NAME_STRING, value));
    }

    public static Builder<PVABoolArray> boolScalarBuilder(boolean... value) {
        return new Builder<PVABoolArray>().value(new PVABoolArray(VALUE_NAME_STRING, value));
    }

    public static Builder<PVAByteArray> byteArrayScalarBuilder(boolean unsigned, byte... value) {
        return new Builder<PVAByteArray>().value(new PVAByteArray(VALUE_NAME_STRING, unsigned, value));
    }

    public static Builder<PVAShortArray> shortArrayScalarBuilder(final boolean unsigned, final short... value) {
        return new Builder<PVAShortArray>().value(new PVAShortArray(VALUE_NAME_STRING, unsigned, value));
    }

    public static Builder<PVAIntArray> intArrayScalarBuilder(boolean unsigned, int... value) {
        return new Builder<PVAIntArray>().value(new PVAIntArray(VALUE_NAME_STRING, unsigned, value));
    }

    public static Builder<PVALongArray> longArrayScalarBuilder(final boolean unsigned, final long... value) {
        return new Builder<PVALongArray>().value(new PVALongArray(VALUE_NAME_STRING, unsigned, value));
    }

    public static Builder<PVAFloatArray> floatArrayScalarBuilder(float... value) {
        return new Builder<PVAFloatArray>().value(new PVAFloatArray(VALUE_NAME_STRING, value));
    }

    public static Builder<PVADoubleArray> doubleArrayScalarBuilder(double... value) {
        return new Builder<PVADoubleArray>().value(new PVADoubleArray(VALUE_NAME_STRING, value));
    }

    public static Builder<PVAStringArray> stringArrayScalarBuilder(String... value) {
        return new Builder<PVAStringArray>().value(new PVAStringArray(VALUE_NAME_STRING, value));
    }
}
