package org.csstudio.opibuilder.converter.model;

public class Edm_activeRadioButtonClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String controlPv;

    @EdmAttributeAn @EdmOptionalAn private EdmColor selectColor;
    @EdmAttributeAn @EdmOptionalAn private EdmColor buttonColor;

    public Edm_activeRadioButtonClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    public EdmColor getSelectColor() {
        return selectColor;
    }
    public EdmColor getButtonColor() {
        return buttonColor;
    }

    /**
     * @return the lineAlarm
     */
    public final String getControlPv() {
        return controlPv;
    }



}