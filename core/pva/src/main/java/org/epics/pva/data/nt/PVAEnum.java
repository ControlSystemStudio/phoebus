package org.epics.pva.data.nt;

import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;

/**
 * Normative enum type
 * 
 * enum_t :=
 * 
 * <ul>
 * <li>structure
 * <ul>
 * <li>int index
 * <li>string[] choices
 */
public class PVAEnum extends PVAStructure {
    /** Type name for enum */
    public static final String ENUM_T = "enum_t";
    private PVAInt index;
    private PVAStringArray choices;

    public PVAEnum(String name, int index, String[] choices) {
        this(name, new PVAInt("index", index), new PVAStringArray("choices", choices));
    }

    public PVAEnum(String name, PVAInt index, PVAStringArray choices) {
        super(name, ENUM_T, index, choices);
        this.index = index;
        this.choices = choices;
    }

    public String enumString() {

        if (this.index != null  && this.choices != null)
        {
            final int i = this.index.get();
            final String[] labels = this.choices.get();
            return i>=0 && i<labels.length ? labels[i] : "Invalid enum <" + i + ">";
        }
        return null;
    }

    public static PVAEnum fromStructure(PVAStructure structure) {

        if (structure.getStructureName().equals(ENUM_T))
        {
            final PVAInt index = structure.get("index");
            final PVAStringArray choices = structure.get("choices");
            return new PVAEnum(structure.getName(), index, choices);
        }
        return null;
    }
}
