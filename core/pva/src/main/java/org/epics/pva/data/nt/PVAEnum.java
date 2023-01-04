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

    public PVAEnum(String name, int index, String[] choices) {
        super(name, "enum_t", new PVAInt("index", index), new PVAStringArray("choices", choices));
    }
}
