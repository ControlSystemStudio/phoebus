package org.phoebus.pv.pvws.models.temp;


//MODELING FOR SERVER AND CLIENT CAN BE IMPLEMENTED AS DEPENDENCY
public class Message {
    private String type;
    private String[] pvs;

    public Message(){

    }

    public Message(String type, String[] pvs) {
        this.type = type;
        this.pvs = pvs;
    }

    public Message(String type){
        this.type = type;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String[] getPvs() { return pvs; }
    public void setPvs(String[] pvs) { this.pvs = pvs; }
}