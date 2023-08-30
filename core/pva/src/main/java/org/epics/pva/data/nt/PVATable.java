/*
 *
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.epics.pva.data.nt;

import org.epics.pva.data.*;

import java.util.*;

/**
 * Normative Table type
 * <p>
 *     NTTable is the EPICS V4 Normative Type suitable for column-oriented tabular datasets.
 * </p>
 * NTTable :=
 * <ul>
 * <li>
 * structure
 * <ul>
 *     <li>string[]   labels            // Very short text describing each field below, i.e. column labels</li>
 *     <li>structure  value
 *         <ul>
 *         <li>{scalar_t[]  colname}0+ // 0 or more scalar array instances, the column values.</li>
 *         </ul>
 *         </li>
 *     <li>string     descriptor  : opt</li>
 *     <li>alarm_t    alarm       : opt</li>
 *     <li>time_t     timeStamp   : opt</li>
 * </ul>
 * </li>
 * </ul>
 */
public class PVATable extends PVAStructure {
    public static final String STRUCT_NAME = "epics:nt/NTTable:1.0";
    private static final String LABELS_NAME = "labels";
    private static final String VALUE_NAME = "value";
    private static final String DESCRIPTOR_NAME = "descriptor";

    private final PVAStringArray labels;
    private final PVAStructure value;
    private final PVAString descriptor;
    private final PVAAlarm alarm;
    private final PVATimeStamp timeStamp;

    public PVAStringArray getLabels() {
        return labels;
    }

    public PVAString getDescriptor() {
        return descriptor;
    }

    public PVAAlarm getAlarm() {
        return alarm;
    }

    public PVATimeStamp getTimeStamp() {
        return timeStamp;
    }


    /**
     * Builder for the {@link PVATable}.
     * <p>
     * For example:
     * PVATable table = PVATable.PVATableBuilder.aPVATable().name("table")
     *                 .alarm(new PVAAlarm())
     *                 .timeStamp(new PVATimeStamp(instant))
     *                 .descriptor("descriptor")
     *                 .addColumn(new PVAStringArray("pvs", "pv1", "pv2", "pv3"))
     *                 .build();
     */
    public static final class PVATableBuilder {
        private String name;
        private List<PVAData> table;
        private String descriptor;
        private PVAAlarm alarm;
        private PVATimeStamp timeStamp;

        private PVATableBuilder() {
        }

        /**
         * Shortcut for new PVATableBuilder()
         * @return new PVATableBuilder()
         */
        public static PVATableBuilder aPVATable() {
            return new PVATableBuilder();
        }

        /**
         * Set the name of the PVATable
         *
         * @param name input name
         * @return the current builder
         */
        public PVATableBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the data of the PVATable, expects a list of {@link PVAData}
         * that implements {@link PVAArray}. The labels and columns of the
         * table will be set from this variable.
         *
         * @param table input table list of {@link PVAData}
         *              that implements {@link PVAArray}
         * @return the current builder
         */
        public PVATableBuilder table(List<PVAData> table) {
            this.table = table;
            return this;
        }

        /**
         * Set the descriptor of the PVATable
         *
         * @param descriptor input descriptor
         * @return the current builder
         */
        public PVATableBuilder descriptor(String descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        /**
         * Set the alarm of the PVATable
         *
         * @param alarm input alarm
         * @return the current builder
         */
        public PVATableBuilder alarm(PVAAlarm alarm) {
            this.alarm = alarm;
            return this;
        }

        /**
         * Set the timeStamp of the PVATable
         *
         * @param timeStamp input {@link PVATimeStamp}
         * @return the current builder
         */
        public PVATableBuilder timeStamp(PVATimeStamp timeStamp) {
            this.timeStamp = timeStamp;
            return this;
        }

        /**
         * Method to extend the current list of data.
         *
         * @param list {@link PVAData} which implements {@link PVAArray}
         * @return current builder
         */
        public <PVA extends PVAData & PVAArray> PVATableBuilder addColumn(PVA list) {
            if (table == null) {
                table = new ArrayList<>();
            }
            this.table.add(list);
            return this;
        }

        /**
         * Build method for the builder. If the non-optional values are
         * set a NullPointerException will be thrown
         *
         * @return The resulting PVATable
         * @throws MustBeArrayException Thrown if the input table contains a
         *                              value which does not extend {@link PVAArray}
         */
        public PVATable build() throws MustBeArrayException {
            if (this.name == null) {
                throw new NullPointerException("The property \"name\" is null. "
                        + "Please set the value by \"name()\". "
                        + "The properties \"name\", \"value\" are required.");
            }
            if (this.table == null) {
                throw new NullPointerException("The property \"table\" is null. "
                        + "Please set the value by \"table()\". "
                        + "The properties \"name\", \"table\" are required.");
            }
            for (PVAData data: this.table) {
                if (!(data instanceof PVAArray)) {
                    throw new MustBeArrayException("The data in the \"table\"" +
                            " is of class " + data.getClass().getSimpleName() +
                            ". But must implement PVAArray.");
                }
            }
            return new PVATable(this);
        }
    }

    private static PVAStringArray labelsFromList(List<PVAData> table) {
        String[] array = new String[table.size()];
        for (int i = 0; i < table.size(); i++) {
            array[i] = table.get(i).getName();
        }
        return new PVAStringArray(LABELS_NAME, array);
    }

    private PVATable(String name, PVAStringArray labels, PVAStructure value,
                     PVAAlarm alarm, PVATimeStamp timeStamp, PVAString descriptor) {
        super(name, STRUCT_NAME, Arrays.stream(new PVAData[] {labels, value, alarm, timeStamp, descriptor})
                        .filter(Objects::nonNull).toArray(PVAData[]::new)
        );
        this.labels = this.get(LABELS_NAME);
        this.value = this.get(VALUE_NAME);
        this.alarm = this.get(PVAAlarm.ALARM_NAME_STRING);
        this.timeStamp = this.get(PVATimeStamp.TIMESTAMP_NAME_STRING);
        this.descriptor = this.get(DESCRIPTOR_NAME);
    }

    private PVATable(PVATableBuilder builder) {
        this(builder.name, labelsFromList(builder.table),
                new PVAStructure(VALUE_NAME, "structure", builder.table),
                builder.alarm, builder.timeStamp, new PVAString(DESCRIPTOR_NAME, builder.descriptor));
    }

    /**
     * Get a specific column of the Table
     *
     * @param label label of the column
     * @return Returns the PVAData holding the column
     * @param <PVA> The type that implements {@link PVAArray}
     */
    public <PVA extends PVAData & PVAArray> PVA getColumn(String label) {
        return this.value.get(label);
    }

    /**
     * Converts from a generic PVAStructure to PVATable
     *
     * @param structure Input structure
     * @return Representative Table
     */
    public static PVATable fromStructure(PVAStructure structure) {
        if (structure == null || !structure.getStructureName().equals(STRUCT_NAME))
            return null;
        final PVAStringArray labels = structure.get(LABELS_NAME);
        final PVAStructure value = structure.get(VALUE_NAME);
        final PVAAlarm alarm = PVAAlarm.getAlarm(structure);
        final PVATimeStamp timeStamp = PVATimeStamp.getTimeStamp(structure);
        final PVAString descriptor = structure.get(DESCRIPTOR_NAME);
        return new PVATable(structure.getName(), labels, value, alarm, timeStamp, descriptor);
    }
}
