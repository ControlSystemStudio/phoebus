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

import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;

/**
 * Normative enum type
 * 
 * An enum_t describes an enumeration. The field is a structure describing a
 * value drawn from a given set of valid values also given.
 * 
 * enum_t :=
 * 
 * <ul>
 * <li>structure
 * <ul>
 * <li>int index
 * <li>string[] choices
 * </ul>
 * </ul>
 */
public class PVAEnum extends PVAStructure {
    /** Type name for enum */
    private static final String ENUM_T = "enum_t";
    private PVAInt index;
    private PVAStringArray choices;

    /**
     * Constructor 
     * @param name Name of the enum
     * @param index The index of the current value of the enumeration in the array choices below.
     * @param choices An array of strings specifying the set of labels for the valid values of the enumeration.
     */
    public PVAEnum(String name, int index, String[] choices) {
        this(name, new PVAInt("index", index), new PVAStringArray("choices", choices));
    }

    /**
     * Constructor 
     * @param name Name of the enum
     * @param index The index of the current value of the enumeration in the array choices below.
     * @param choices An array of strings specifying the set of labels for the valid values of the enumeration.
     */
    public PVAEnum(String name, PVAInt index, PVAStringArray choices) {
        super(name, ENUM_T, index, choices);
        this.index = index;
        this.choices = choices;
    }

    /**
     * String of the enum output
     * 
     * @return The resulting string of the enum_t
     */
    public String enumString() {

        if (this.index != null && this.choices != null) {
            final int i = this.index.get();
            final String[] labels = this.choices.get();
            return i >= 0 && i < labels.length ? labels[i] : "Invalid enum <" + i + ">";
        }
        return null;
    }

    /**
     * Converts from a generic PVAStruture to PVAEnum
     * 
     * @param structure Input structure
     * @return Representative Enum
     */
    public static PVAEnum fromStructure(PVAStructure structure) {
        if (structure != null && structure.getStructureName().equals(ENUM_T)) {
            final PVAInt index = structure.get("index");
            final PVAStringArray choices = structure.get("choices");
            return new PVAEnum(structure.getName(), index, choices);
        }
        return null;
    }

}
