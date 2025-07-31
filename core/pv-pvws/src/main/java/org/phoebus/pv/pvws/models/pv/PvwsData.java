package org.phoebus.pv.pvws.models.pv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PvwsData {


    private String type;
    private String pv;
    private String severity;
    private String vtype;
    private String units;
    private String description;
    private String text;
    private String labels; // WILL THIS BE A STRING?


    private Object value;
    private int seconds;
    private int nanos;
    private int precision;

    private double min;
    private double max;
    private double warn_low;
    private double warn_high;
    private double alarm_low;
    private double alarm_high;
    private boolean readonly;


    public PvwsData() {
    }


    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public boolean getReadonly() {
        return readonly;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPv() {
        return pv;
    }

    public void setPv(String pv) {
        this.pv = pv;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getVtype() {
        return vtype;
    }

    public void setVtype(String vtype) {
        this.vtype = vtype;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getNanos() {
        return nanos;
    }

    public void setNanos(int nanos) {
        this.nanos = nanos;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getWarn_low() {
        return warn_low;
    }

    public void setWarn_low(double warn_low) {
        this.warn_low = warn_low;
    }

    public double getWarn_high() {
        return warn_high;
    }

    public void setWarn_high(double warn_high) {
        this.warn_high = warn_high;
    }

    public double getAlarm_low() {
        return alarm_low;
    }

    public void setAlarm_low(double alarm_low) {
        this.alarm_low = alarm_low;
    }

    public double getAlarm_high() {
        return alarm_high;
    }

    public void setAlarm_high(double alarm_high) {
        this.alarm_high = alarm_high;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getLabels() {
        return this.labels;
    }


}



