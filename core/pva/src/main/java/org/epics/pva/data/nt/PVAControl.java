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
    public static final String CONTROL_NAME_STRING = "control";

    /**
     * Setting all parameters
     * 
     * @param limitLow
     * @param limitHigh
     * @param minStep
     */
    public PVAControl(double limitLow, double limitHigh, double minStep) {
        super(CONTROL_NAME_STRING, "control_t",
                new PVADouble("limitLow", limitLow),
                new PVADouble("limitHigh", limitHigh),
                new PVADouble("minStep", minStep));
    }

}
