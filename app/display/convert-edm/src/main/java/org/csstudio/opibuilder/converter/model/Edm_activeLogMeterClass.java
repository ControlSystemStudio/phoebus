package org.csstudio.opibuilder.converter.model;

public class Edm_activeLogMeterClass extends Edm_activeMeterClass {

    @EdmAttributeAn @EdmOptionalAn private String scaleFormat;

    public Edm_activeLogMeterClass(EdmEntity genericEntity)
            throws EdmException {
        super(genericEntity);
        System.out.println("Found a meter");
    }

    public String getScaleFormat() {
        return scaleFormat;
    }
}
