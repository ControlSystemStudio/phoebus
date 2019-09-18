package org.csstudio.opibuilder.converter.model;

public class Edm_activeShapeClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private int lineWidth;

    public Edm_activeShapeClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    public int getLineWidth() {
        return lineWidth;
    }

}
