package org.phoebus.alarm.logging;

public class AlarmTalkMessage {

    private String severity;
    private boolean standout;
    private String talk;

    public AlarmTalkMessage() {
        super();
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isStandout() {
        return standout;
    }

    public void setStandout(boolean standout) {
        this.standout = standout;
    }

    public String getTalk() {
        return talk;
    }

    public void setTalk(String talk) {
        this.talk = talk;
    }

}
