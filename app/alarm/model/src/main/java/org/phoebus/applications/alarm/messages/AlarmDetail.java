package org.phoebus.applications.alarm.messages;

public class AlarmDetail {

    private String title;
    private String action;

    public AlarmDetail(String title, String action) {
        super();
        this.title = title;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

}
