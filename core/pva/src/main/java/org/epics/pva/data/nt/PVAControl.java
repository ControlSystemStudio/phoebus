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
    public static final String CONTROL_T = "control_t";

    /**
     * Setting all parameters
     * 
     * @param limitLow
     * @param limitHigh
     * @param minStep
     */
    public PVAControl(double limitLow, double limitHigh, double minStep) {
        this(new PVADouble("limitLow", limitLow),
                new PVADouble("limitHigh", limitHigh),
                new PVADouble("minStep", minStep));
    }

    /**
     * Setting all parameters
     * 
     * @param limitLow
     * @param limitHigh
     * @param minStep
     */
    public PVAControl(PVADouble limitLow, PVADouble limitHigh, PVADouble minStep) {
        super(CONTROL_NAME_STRING, CONTROL_T,
                limitLow,
                limitHigh,
                minStep);
    }

    /**
     * Conversion from structure to PVAControl
     * 
     * @param structure Potential "control_t" structure
     * @return PVAControl or <code>null</code>
     */
    public static PVAControl fromStructure(PVAStructure structure) {
        if (structure.getStructureName().equals(CONTROL_T)) {
            final PVADouble limitLow = structure.get("limitLow");
            final PVADouble limitHigh = structure.get("limitHigh");
            final PVADouble minStep = structure.get("minStep");
            return new PVAControl(limitLow, limitHigh, minStep);
        }
        return null;
    }
}
