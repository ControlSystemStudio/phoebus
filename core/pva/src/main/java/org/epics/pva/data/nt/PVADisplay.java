/*
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
 *
 */
package org.epics.pva.data.nt;

import java.util.stream.Stream;

import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;

/**
 * Normative Display type
 * 
 * A display_t is a structure that describes some typical attributes of a
 * numerical value that are of interest when displaying the value on a computer
 * screen or similar medium.
 * 
 * <p>
 * display_t :=
 * <ul>
 * <li>structure
 * <ul>
 * <li>double limitLow
 * <li>double limitHigh
 * <li>string description
 * <li>string units
 * <li>int precision
 * <li>enum_t form(3)
 * <ul>
 * <li>int index
 * <li>string[] choices ["Default", "String", "Binary", "Decimal", "Hex",
 * "Exponential", "Engineering"]
 * </ul>
 * </ul>
 * </ul>
 */
public class PVADisplay extends PVAStructure {
    private static final String DISPLAY_NAME_STRING = "display";
    private static final String DISPLAY_T = "display_t";

    /**
     * An enumeration to specify formatting a value to be displayed. By default, a
     * floating point number is formatted with the number of decimal points defined
     * in the precision field.
     */
    public enum Form {
        /** 
         * Default Formatting
         */
        DEFAULT,
        /** 
         * String Format
         */
        STRING,
        /** 
         * Binary Format
         */
        BINARY,
        /** 
         * Decimal Format
         */
        DECIMAL,
        /** 
         * Hex Format
         */
        HEX,
        /** 
         * Exponential Format
         */
        EXPONENTIAL,
        /** 
         * Engineering Format
         */
        ENGINEERING
    }

    private static String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }

    /**
     * Construct a display_t normative type PVAStructure
     * 
     * @param limitLow    The lower bound of range within which the value must be
     *                    set, to be presented to a user.
     * @param limitHigh   The upper bound of range within which the value must be
     *                    set, to be presented to a user.
     * @param description A textual summary of the variable that the value
     *                    quantifies.
     * @param units       The units for the value field.
     * @param precision   Number of decimal points that are displayed when
     *                    formatting a floating point number.
     * @param form        An enumeration to specify formatting a value to be
     *                    displayed.
     */
    public PVADisplay(double limitLow, double limitHigh, String description, String units, int precision,
            Form form) {
        this(new PVADouble("limitLow", limitLow),
                new PVADouble("limitHigh", limitHigh),
                new PVAString("description", description),
                new PVAString("units", units),
                new PVAInt("precision", precision),
                new PVAEnum("form", form.ordinal(), Stream.of(Form.values()).map(Form::name)
                        .map(PVADisplay::capitalizeFirstLetter).toArray(String[]::new)));

    }

    /**
     * Construct a display_t normative type PVAStructure
     * 
     * @param limitLow    The lower bound of range within which the value must be
     *                    set, to be presented to a user.
     * @param limitHigh   The upper bound of range within which the value must be
     *                    set, to be presented to a user.
     * @param description A textual summary of the variable that the value
     *                    quantifies.
     * @param units       The units for the value field.
     * @param precision   Number of decimal points that are displayed when
     *                    formatting a floating point number.
     * @param form        An enumeration to specify formatting a value to be
     *                    displayed.
     */
    public PVADisplay(PVADouble limitLow, PVADouble limitHigh, PVAString description, PVAString units, PVAInt precision,
            PVAEnum form) {
        super(DISPLAY_NAME_STRING, DISPLAY_T,
                limitLow,
                limitHigh,
                description,
                units,
                precision,
                form);

    }

    /**
     * Conversion from structure to PVADisplay
     * 
     * @param structure Potential "display_t" structure
     * @return PVADisplay or <code>null</code>
     */
    public static PVADisplay fromStructure(PVAStructure structure) {
        // Epics Iocs do not always use the DISPLAY_T type name
        if (structure != null && structure.getName().equals(DISPLAY_NAME_STRING)) {
            return new PVADisplay(
                    structure.get("limitLow"),
                    structure.get("limitHigh"),
                    structure.get("description"),
                    structure.get("units"),
                    structure.get("precision"),
                    PVAEnum.fromStructure(structure.get("form")));
        }
        return null;
    }

    /**
     * Get Display from a PVAStructure
     * 
     * @param structure Structure containing Display
     * @return PVADisplay or <code>null</code>
     */
    public static PVADisplay getDisplay(PVAStructure structure) {
        PVAStructure displayStructure = structure.get(DISPLAY_NAME_STRING);
        if (displayStructure != null) {
            return fromStructure(displayStructure);
        }
        return null;
    }
}
