package org.csstudio.opibuilder.converter.model;

/**EDM Text Control
 * @author Xihui Chen
 *
 */
public class Edm_activeXTextDspClass extends Edm_activeXTextDspClass_noedit {

    @EdmAttributeAn @EdmOptionalAn private boolean editable;
    @EdmAttributeAn @EdmOptionalAn private boolean date;
    @EdmAttributeAn @EdmOptionalAn private boolean file;
    @EdmAttributeAn @EdmOptionalAn private String defDir;
    @EdmAttributeAn @EdmOptionalAn private String fileComponent;




    public Edm_activeXTextDspClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }



    public boolean isEditable() {
        return editable;
    }



    public boolean isDate() {
        return date;
    }



    public boolean isFile() {
        return file;
    }



    public String getDefDir() {
        return defDir;
    }

    public String getFileComponent() {
        if(getAttribute("fileComponent").isExistInEDL())
            return fileComponent;
        return "";
    }


}