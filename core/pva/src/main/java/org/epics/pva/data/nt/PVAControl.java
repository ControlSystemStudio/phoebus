package org.epics.pva.data.nt;

import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAStructure;

/**
 * Normative control type
 * 
 * control_t :=
 * <ul>
 * <li>structure
 * <ul>
 * <li>double limitLow
 * <li>double limitHigh
 * <li>double minStep
 * 
 */
public class PVAControl extends PVAStructure {

    /**
     * Setting all parameters
     * 
     * @param name
     * @param limitLow
     * @param limitHigh
     * @param minStep
     */
    public PVAControl(String name, double limitLow, double limitHigh, double minStep) {
        super(name, "control_t",
                new PVADouble("limitLow", limitLow),
                new PVADouble("limitHigh", limitHigh),
                new PVADouble("minStep", minStep));
    }

}
